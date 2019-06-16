package com.diploma.app.service;

import com.diploma.app.model.*;
import com.diploma.app.repository.ConnectionRepository;
import com.diploma.app.repository.NodeRepository;
import com.diploma.app.repository.RoadRepository;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Optional.ofNullable;

@Service
public class TransportationServiceImpl {

    private static final String LOCAL = "Loc";
    private static final String REGIONAL = "Reg";
    private static final String NATIONAL = "Nat";
    private static final String SUPPLIER = "Sup";

    private final RoadRepository roadRepository;
    private final ConnectionRepository connectionRepository;
    private final NodeRepository nodeRepository;

    private static final BigDecimal TRANSPORT_PRICE = BigDecimal.valueOf(0.678);

    @Autowired
    public TransportationServiceImpl(RoadRepository roadRepository, ConnectionRepository connectionRepository, NodeRepository nodeRepository) {
        this.roadRepository = roadRepository;
        this.connectionRepository = connectionRepository;
        this.nodeRepository = nodeRepository;
    }

    public ResultDto process(InputStream inputStream, Integer regionalWhAmount) throws IOException, InvalidFormatException {
        Workbook workbook = WorkbookFactory.create(inputStream);

        Map<String, List<Connection>> localToRegionalConnections = loadInitConnections(workbook,
                "RegionalToLocalDistance");
        Map<String, List<Connection>> regionalToNationalConnections = loadInitConnections(workbook,
                "NationalToRegionalDistance");
        Map<String, List<Connection>> nationalToSupplierConnections = loadInitConnections(workbook,
                "SuppliersToNationalDistance");

        setInitPrices(workbook, localToRegionalConnections);
        Map<String, Connection> localToRegionalCheapestConnections = getCheapestConnections(localToRegionalConnections);
        Map<String, Connection> regionalToNationalClosestConnections = getClosestConnections(regionalToNationalConnections);
        Map<String, Connection> nationalToSupplierClosestConnections = getClosestConnections(nationalToSupplierConnections);

        printSolution(localToRegionalCheapestConnections,
                regionalToNationalClosestConnections,
                nationalToSupplierClosestConnections);

        ResultDto resultDto = newApproach(localToRegionalCheapestConnections, regionalToNationalClosestConnections,
                nationalToSupplierClosestConnections, (HashMap<String, List<Connection>>) localToRegionalConnections,
                ofNullable(regionalWhAmount).orElse(regionalToNationalClosestConnections.size()));

        return resultDto;
//        transportationService.saveSolutionToFile(localToRegionalCheapestConnections,
//                regionalToNationalClosestConnections, nationalToSupplierClosestConnections, workbook);
    }


    private ResultDto newApproach(Map<String, Connection> localToRegionalCheapestConnections,
                                   Map<String, Connection> regionalToNationalClosestConnections,
                                   Map<String, Connection> nationalToSupplierClosestConnections,
                                   HashMap<String, List<Connection>> localToRegionalConnections,
                                   Integer regionalWhAmount) {

        List<Road> roads = createRoads(localToRegionalCheapestConnections,
                regionalToNationalClosestConnections,
                nationalToSupplierClosestConnections);

        List<Road> result;
        Map<String, Connection> localRegional = localToRegionalCheapestConnections;

        Map<String, BigDecimal> regionalToDistanceMap = regionalToNationalClosestConnections.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getDistance()
                        .add(nationalToSupplierClosestConnections.get(entry.getValue().getSourceNode().getName()).getDistance())));

        if (regionalWhAmount > 0) {
            Set<String> regionalWhs = regionalToNationalClosestConnections.keySet();
            int regionalCitiesExcluded = regionalToNationalClosestConnections.size() - regionalWhAmount;

            List<List<String>> excludedRegionalCities = new ArrayList<>();
            findPermutations(new ArrayList<>(regionalWhs), new String[regionalCitiesExcluded], 0, regionalWhs.size() - 1,
                    0, regionalCitiesExcluded, excludedRegionalCities);

            Map<List<String>, BigDecimal> minDemandSums = new HashMap<>();


            excludedRegionalCities.forEach(cities -> {
                Map<String, List<Connection>> temp = localToRegionalConnections.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>(entry.getValue())));

                temp.values().forEach(connections -> connections.removeIf(connection -> cities.contains(connection.getSourceNode().getName())));
                BigDecimal sumPriceOfCheapestConnections = getSumPriceOfCheapestConnections(temp, regionalToDistanceMap);
                minDemandSums.put(cities, sumPriceOfCheapestConnections);
            });

            List<String> excludedRegionalCitiesFin = Collections.min(minDemandSums.entrySet(), Map.Entry.comparingByValue()).getKey();

            localToRegionalConnections.values().forEach(connections -> connections
                    .removeIf(connection -> excludedRegionalCitiesFin.contains(connection.getSourceNode().getName())));

            Map<String, Connection> cheapestConnections = getCheapestConnections(localToRegionalConnections);
            List<Road> roadsResult = createRoads(cheapestConnections, regionalToNationalClosestConnections, nationalToSupplierClosestConnections);
            localRegional = cheapestConnections;
            saveData(cheapestConnections,
                    regionalToNationalClosestConnections,
                    nationalToSupplierClosestConnections,
                    roadsResult);

            result = roadsResult;
        } else {
            result = roads;
        }

        ResultDto resultDto = new ResultDto();
        Map<String, List<Connection>> regionalToLocal = localRegional.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.groupingBy(entry -> entry.getSourceNode().getName()));

        List<RegionalToLocalDto> regionalToLocalDtos = regionalToLocal.entrySet().stream()
                .map(entry -> new RegionalToLocalDto(entry.getKey(), entry.getValue().stream().map(Connection::getDestinationNode)
                        .map(Node::getName).collect(Collectors.toList()))).collect(Collectors.toList());

        resultDto.setRegionalToLocalDtos(regionalToLocalDtos);
        System.out.println(regionalToLocalDtos);

        BigDecimal sumConnection = result.stream().map(this::getSumConn).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        BigDecimal sumToRegionalConnection = result.stream().map(this::getSumToRegionalConn).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        BigDecimal sumToNationalConnection = result.stream().map(this::getSumToNatConn).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        resultDto.setSumConnection(sumConnection);
        resultDto.setSumToRegionalConnection(sumToRegionalConnection);
        resultDto.setSumToNationalConnection(sumToNationalConnection);

        List<List<String>> roadMatrix = result.stream().map(road -> {
            Connection regionalToNationalConn = road.getRegionalToNationalConn();
            System.out.println(road.getNationalToSupplierConn().getSourceNode() + " -> " + regionalToNationalConn.getSourceNode()
                    + " -> " + regionalToNationalConn.getDestinationNode() + " -> " + road.getLocalToRegionalConn().getDestinationNode());
            return Arrays.asList(road.getNationalToSupplierConn().getSourceNode().getName(), regionalToNationalConn.getSourceNode().getName(),
                    regionalToNationalConn.getDestinationNode().getName(), road.getLocalToRegionalConn().getDestinationNode().getName());
        }).collect(Collectors.toList());

        Map<String, String> citiesMap = new HashMap<>();
        result.forEach(road -> {
            Connection localToRegionalConn = road.getLocalToRegionalConn();
            Connection nationalToSupplierConn = road.getNationalToSupplierConn();
            citiesMap.put(localToRegionalConn.getDestinationNode().getName(), LOCAL);
            citiesMap.put(localToRegionalConn.getSourceNode().getName(), REGIONAL);
            citiesMap.put(nationalToSupplierConn.getDestinationNode().getName(), NATIONAL);
            citiesMap.put(nationalToSupplierConn.getSourceNode().getName(), SUPPLIER);
        });

        resultDto.setRoadsMatrix(roadMatrix);
        resultDto.setCitiesRoadsDto(new CitiesRoadsDto(citiesMap, result));

        return resultDto;
    }

    private BigDecimal getSumConn(Road road) {
        Connection localToRegionalConn = road.getLocalToRegionalConn();
        Connection regionalToNationalConn = road.getRegionalToNationalConn();
        Connection nationalToSupplierConn = road.getNationalToSupplierConn();
        return localToRegionalConn.getDestinationNode().getDemand().multiply(regionalToNationalConn.getDistance()
                .add(nationalToSupplierConn.getDistance().add(localToRegionalConn.getDistance()))).multiply(TRANSPORT_PRICE);
    }

    private BigDecimal getSumToNatConn(Road road) {
        Connection localToRegionalConn = road.getLocalToRegionalConn();
        Connection regionalToNationalConn = road.getRegionalToNationalConn();
        return localToRegionalConn.getDestinationNode().getDemand().multiply(regionalToNationalConn.getDistance()
                .add(localToRegionalConn.getDistance())).multiply(TRANSPORT_PRICE);
    }

    private BigDecimal getSumToRegionalConn(Road road) {
        Connection localToRegionalConn = road.getLocalToRegionalConn();
        return localToRegionalConn.getDestinationNode().getDemand().multiply(localToRegionalConn.getDistance()).multiply(TRANSPORT_PRICE);
    }

    private BigDecimal getSumPriceOfCheapestConnections(Map<String, List<Connection>> mappedConnections, Map<String, BigDecimal> regionalToDistanceMap) {
        return mappedConnections.entrySet().stream().map(entry -> entry.getValue().stream().map(con -> con.getDestinationNode().getDemand()
                .multiply(con.getDistance().add(regionalToDistanceMap.get(con.getSourceNode().getName())))
                .multiply(TRANSPORT_PRICE)).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private void findPermutations(List<String> wareHouses, String[] temp, int start,
                                  int end, int index, int r, List<List<String>> result) {
        // Current combination is ready to be printed, print it
        if (index == r) {
            List<String> tempList = new ArrayList<>();
            for (int j = 0; j < r; j++) {
                tempList.add(temp[j]);
            }
            result.add(tempList);
            return;
        }

        for (int i = start; i <= end && end - i + 1 >= r - index; i++) {
            temp[index] = wareHouses.get(i);
            findPermutations(wareHouses, temp, i + 1, end, index + 1, r, result);
        }
    }

    private Map<String, Connection> getClosestConnections(Map<String, List<Connection>> mappedConnections) {
        System.out.println(mappedConnections.size() + " Local Cities");
        Map<String, Connection> resultMap = new HashMap<>();

        mappedConnections.forEach((k, v) -> resultMap.put(k, v.stream().min(Comparator.comparingDouble(connection ->
                connection.getDistance().doubleValue())).orElse(null)));

        return resultMap;
    }

    private Map<String, Connection> getCheapestConnections(Map<String, List<Connection>> mappedConnections) {
        Map<String, Connection> resultMap = new HashMap<>();

        mappedConnections.forEach((k, v) -> {
            List<BigDecimal> demands = v.stream().map(con -> con.getDestinationNode().getDemand()
                    .multiply(con.getDistance()).multiply(TRANSPORT_PRICE)).collect(Collectors.toList());
            BigDecimal minDemand = demands.stream().min(Comparator.naturalOrder()).orElse(null);
            Connection minConnection = v.stream().filter(con -> con.getDestinationNode().getDemand()
                    .multiply(con.getDistance()).multiply(TRANSPORT_PRICE).compareTo(minDemand) == 0).findFirst().orElse(null);
            resultMap.put(k, minConnection);
        });
        return resultMap;
    }

    private List<Road> createRoads(Map<String, Connection> localToRegionalCheapestConnections,
                                   Map<String, Connection> regionalToNationalClosestConnections,
                                   Map<String, Connection> nationalToSupplierClosestConnections) {
        return localToRegionalCheapestConnections.entrySet().stream().map(entry -> {
            Connection localToRegionalConn = entry.getValue();
            Node localSourceNode = localToRegionalConn.getSourceNode();
            Connection regionalToNationalConn = regionalToNationalClosestConnections.get(localSourceNode.getName());
            Node regionalSourceNode = regionalToNationalConn.getSourceNode();
            Connection nationalToSupplierConn = nationalToSupplierClosestConnections.get(regionalSourceNode.getName());

            BigDecimal nationalToSupplierDistance = nationalToSupplierConn.getDistance();
            BigDecimal regionalToNationalDistance = regionalToNationalConn.getDistance();
            BigDecimal localToRegionalDistance = localToRegionalConn.getDistance();
            BigDecimal totalDistance = nationalToSupplierDistance.add(regionalToNationalDistance).add(localToRegionalDistance);

            return new Road(totalDistance, localToRegionalConn, regionalToNationalConn, nationalToSupplierConn);
        }).collect(Collectors.toList());
    }

    @Transactional
    public void saveData(Map<String, Connection> localToRegionalCheapestConnections,
                         Map<String, Connection> regionalToNationalClosestConnections,
                         Map<String, Connection> nationalToSupplierClosestConnections,
                         List<Road> roads) {
        regionalToNationalClosestConnections.values().forEach(connectionRepository::save);
        nationalToSupplierClosestConnections.values().forEach(connectionRepository::save);
        localToRegionalCheapestConnections.values().forEach(connectionRepository::save);
        roads.forEach(roadRepository::save);
        System.out.println("Stored successfully!");
    }

    private void saveIfNotPresent(Node node) {
        Node byName = nodeRepository.getByName(node.getName());
        if (!ofNullable(byName).isPresent()) {
            nodeRepository.save(node);
        }
    }

    private void printSolution(Map<String, Connection> localToRegionalCheapestConnections,
                               Map<String, Connection> regionalToNationalClosestConnections,
                               Map<String, Connection> nationalToSupplierClosestConnections) {
        localToRegionalCheapestConnections.forEach((localName, localToRegionalConn) -> {
            String regionalName = localToRegionalConn.getSourceNode().getName();
            Connection regionalToNationalConn = regionalToNationalClosestConnections.get(regionalName);
            String nationalName = regionalToNationalConn.getSourceNode().getName();
            Connection nationalToSupplierConn = nationalToSupplierClosestConnections.get(nationalName);
            String supplierName = nationalToSupplierConn.getSourceNode().getName();
            BigDecimal nationalToSupplierDistance = nationalToSupplierConn.getDistance();
            BigDecimal regionalToNationalDistance = regionalToNationalConn.getDistance();
            BigDecimal localToRegionalDistance = localToRegionalConn.getDistance();
            BigDecimal totalDistance = nationalToSupplierDistance.add(regionalToNationalDistance).add(localToRegionalDistance);

            System.out.println(supplierName + " " + nationalToSupplierDistance + " km -> "
                    + nationalName + " " + regionalToNationalDistance + " km -> "
                    + regionalName + " " + localToRegionalDistance + " km -> "
                    + localName + "| Total distance is " + totalDistance);

            System.out.println();
        });
    }

    private void saveSolutionToFile(Map<String, Connection> localToRegionalCheapestConnections,
                                    Map<String, Connection> regionalToNationalClosestConnections,
                                    Map<String, Connection> nationalToSupplierClosestConnections,
                                    Workbook workbook) {
        makeSheet(localToRegionalCheapestConnections, workbook, "RegionalLocalSolution");
        Sheet nationalRegionalSheet = makeSheet(regionalToNationalClosestConnections, workbook, "NationalRegionalSolution");
        AtomicInteger natIndex = new AtomicInteger(1);

        nationalToSupplierClosestConnections.forEach((name, conn) -> {
            int i = natIndex.getAndIncrement();
            int nationalIndex = nationalToSupplierClosestConnections.size() + i;
            Row row = nationalRegionalSheet.createRow(regionalToNationalClosestConnections.size() + i);
            row.createCell(nationalIndex).setCellValue(conn.getDistance().doubleValue());
            row.createCell(0).setCellValue(name);
            nationalRegionalSheet.getRow(0).createCell(nationalIndex).setCellValue(conn.getSourceNode().getName());
        });

        saveWorkbookToFile(workbook, "TestFile.xlsx");
    }

    private Sheet makeSheet(Map<String, Connection> localToRegionalCheapestConnections, Workbook workbook, String sheetName) {
        Sheet sheet = workbook.createSheet(sheetName);

        AtomicInteger headerIndex = new AtomicInteger(1);
        Map<Integer, String> regionalNodes = localToRegionalCheapestConnections.values().stream()
                .map(Connection::getSourceNode)
                .map(Node::getName).distinct().collect(Collectors.toMap(k -> headerIndex.getAndIncrement(), Function.identity()));
        Row headerRow = sheet.createRow(0);
        regionalNodes.forEach((i, node) -> headerRow.createCell(i).setCellValue(node));

        AtomicInteger index = new AtomicInteger(1);

        localToRegionalCheapestConnections.forEach((name, conn) -> {
            Row row = sheet.createRow(index.getAndIncrement());
            row.createCell(0).setCellValue(name);

            Integer regionalIndex = regionalNodes.entrySet().stream()
                    .filter(e -> e.getValue().equals(conn.getSourceNode().getName()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);

            row.createCell(regionalIndex).setCellValue(conn.getDistance().doubleValue());
        });
        return sheet;
    }

    private void saveWorkbookToFile(Workbook workbook, String fileName) {
        try {
            FileOutputStream fileOut = new FileOutputStream(fileName);
            workbook.write(fileOut);
            fileOut.close();
        } catch (IOException e) {
        }
    }

    private void setInitPrices(Workbook workbook, Map<String, List<Connection>> connections) {
        Sheet demandSheet = workbook.getSheet("Demand");

        demandSheet.forEach(row -> {
            if (row.getRowNum() > 1) {
                IntStream.range(0, row.getLastCellNum()).forEach(i -> {
                    String nodeName = row.getCell(0).getStringCellValue();
                    BigDecimal teaDemand = new BigDecimal(getStringValue(row, 1));
                    BigDecimal dryMixesDemand = new BigDecimal(getStringValue(row, 3));
                    BigDecimal sousesDemand = new BigDecimal(getStringValue(row, 5));

                    ofNullable(connections.get(nodeName)).ifPresent(cons -> cons
                            .forEach(con -> con.getDestinationNode().setDemand(teaDemand.add(dryMixesDemand).add(sousesDemand))));
                });
            }
        });
    }

    private static String getStringValue(final Row row, final int index) {
        Cell cell = row.getCell(index);
        if (cell != null) {
            try {
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new DataFormatter().formatCellValue(cell);
                }
                return String.valueOf(cell.getNumericCellValue());
            } catch (IllegalStateException e) {
                return cell.getStringCellValue();
            }
        }
        return null;
    }

    private Map<String, List<Connection>> loadInitConnections(Workbook workbook, String sheetName) throws IOException, InvalidFormatException {
        Sheet distanceSheet = workbook.getSheet(sheetName);
        List<Connection> connections = new ArrayList<>();
        Row headerRow = distanceSheet.getRow(0);
        Map<Integer, String> headerToIndex = new HashMap<>();

        IntStream.range(1, headerRow.getLastCellNum()).forEach(i -> {
            String nodeName = headerRow.getCell(i).getStringCellValue();
            headerToIndex.put(i, nodeName);
        });

        distanceSheet.forEach(row -> {
            if (row.getRowNum() != 0) {
                IntStream.range(1, row.getLastCellNum()).forEach(i -> {
                    String localNodeName = row.getCell(0).getStringCellValue();
                    String regionalNodeName = headerToIndex.get(i);
                    if (connections.stream().noneMatch(c -> c.getDestinationNode().getName().equals(localNodeName)
                            && c.getSourceNode().getName().equals(regionalNodeName))) {
                        connections.add(new Connection(new Node(regionalNodeName), new Node(localNodeName),
                                new BigDecimal(row.getCell(i).getNumericCellValue())));
                    }
                });
            }
        });

        return connections.stream().collect(Collectors.groupingBy(c -> c.getDestinationNode().getName()));
    }
}
