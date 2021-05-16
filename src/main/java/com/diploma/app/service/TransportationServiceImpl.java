package com.diploma.app.service;

import com.diploma.app.model.*;
import com.diploma.app.repository.ConnectionRepository;
import com.diploma.app.repository.NodeRepository;
import com.diploma.app.repository.RoadRepository;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    private final RoadRepository roadRepository;
    private final ConnectionRepository connectionRepository;
    private final NodeRepository nodeRepository;

    private static final BigDecimal TRANSPORT_PRICE = BigDecimal.valueOf(0.678);
    private static int fileNumber = 0;

    @Autowired
    public TransportationServiceImpl(
            RoadRepository roadRepository, ConnectionRepository connectionRepository, NodeRepository nodeRepository)
    {
        this.roadRepository = roadRepository;
        this.connectionRepository = connectionRepository;
        this.nodeRepository = nodeRepository;
    }

    public ResultDto calculateResult(Integer regionalWhAmount) throws IOException, InvalidFormatException {
        Map<String, List<Connection>> regionalToLocal =
                retrieveConnectionsGroupedByDestination(NodeType.REGIONAL, NodeType.LOCAL);
        Map<String, List<Connection>> nationalToRegional =
                retrieveConnectionsGroupedByDestination(NodeType.NATIONAL, NodeType.REGIONAL);
        Map<String, List<Connection>> supplierToNational =
                retrieveConnectionsGroupedByDestination(NodeType.SUPPLIER, NodeType.NATIONAL);

        Map<String, Connection> localToRegionalCheapestConnections = getCheapestConnections(regionalToLocal);
        Map<String, Connection> regionalToNationalClosestConnections = getClosestConnections(nationalToRegional);
        Map<String, Connection> nationalToSupplierClosestConnections = getClosestConnections(supplierToNational);

        printSolution(localToRegionalCheapestConnections,
                regionalToNationalClosestConnections,
                nationalToSupplierClosestConnections);

        ResultDto resultDto = newApproach(localToRegionalCheapestConnections, regionalToNationalClosestConnections,
                nationalToSupplierClosestConnections, (HashMap<String, List<Connection>>) regionalToLocal,
                ofNullable(regionalWhAmount).orElse(regionalToNationalClosestConnections.size()));

        String fileName = String.format("download/matrix%d.xlsx", fileNumber++);
        
        saveDistanceMapToFile(localToRegionalCheapestConnections,
                regionalToNationalClosestConnections, nationalToSupplierClosestConnections, fileName);

        resultDto.setDownloadFilename(fileName);

        return resultDto;
    }

    public void processUpload(InputStream inputStream) throws IOException, InvalidFormatException {
        Workbook workbook = WorkbookFactory.create(inputStream);

        List<Connection> localToRegionalConnections = parseConnections(workbook,
                "RegionalToLocalDistance", NodeType.REGIONAL, NodeType.LOCAL);
        List<Connection> regionalToNationalConnections = parseConnections(workbook,
                "NationalToRegionalDistance", NodeType.NATIONAL, NodeType.REGIONAL);
        List<Connection> nationalToSupplierConnections = parseConnections(workbook,
                "SuppliersToNationalDistance", NodeType.SUPPLIER, NodeType.NATIONAL);

        localToRegionalConnections.forEach(connectionRepository::save);
        regionalToNationalConnections.forEach(connectionRepository::save);
        nationalToSupplierConnections.forEach(connectionRepository::save);

        setTotalDemands(workbook, "demand");
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

        if (regionalWhAmount > 0) {
            Set<String> regionalWhs = regionalToNationalClosestConnections.keySet();
            int regionalCitiesExcluded = regionalToNationalClosestConnections.size() - regionalWhAmount;

            List<List<String>> excludedRegionalCities = new ArrayList<>();
            findPermutations(new ArrayList<>(regionalWhs), new String[regionalCitiesExcluded], 0, regionalWhs.size() - 1,
                    0, regionalCitiesExcluded, excludedRegionalCities);

            Map<List<String>, BigDecimal> minDemandSums = new HashMap<>();

            Map<String, BigDecimal> regionalToDistanceMap = regionalToNationalClosestConnections.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getDistance()
                            .add(nationalToSupplierClosestConnections.get(entry.getValue().getSourceNode().getName()).getDistance())));

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
/*
            saveData(cheapestConnections,
                    regionalToNationalClosestConnections,
                    nationalToSupplierClosestConnections,
                    roadsResult);
*/

            result = roadsResult;
        } else {
            result = roads;
        }

        ResultDto resultDto = new ResultDto();
        Map<String, List<Connection>> regionalToLocal = localRegional.values().stream().collect(Collectors.groupingBy(entry -> entry.getSourceNode().getName()));

        regionalToLocal.forEach((key, value) -> {
            BigDecimal priceSumForRegional = value.stream().map(this::calcConnectionPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            System.out.println("Price sum for regional city " + key + " = " + priceSumForRegional);
        });

        List<RegionalToLocalDto> regionalToLocalDtos = regionalToLocal.entrySet().stream()
                .map(entry -> new RegionalToLocalDto(entry.getKey(), entry.getValue().stream().map(Connection::getDestinationNode)
                        .map(Node::getName).collect(Collectors.toList()))).collect(Collectors.toList());
        resultDto.setRegionalToLocalDtos(regionalToLocalDtos);

        BigDecimal sumConnection = result.stream().map(this::getSumConn).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        BigDecimal sumToRegionalConnection = result.stream().map(this::getSumToRegionalConn).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        BigDecimal sumToNationalConnection = result.stream().map(this::getSumToNatConn).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        resultDto.setSumConnection(sumConnection);
        resultDto.setSumToRegionalConnection(sumToRegionalConnection);
        resultDto.setSumToNationalConnection(sumToNationalConnection);

        List<List<String>> roadMatrix = result.stream().map(road -> {
            Connection locToReg = road.getLocalToRegionalConn();
            Connection regToNat = road.getRegionalToNationalConn();
            Connection natToSup = road.getNationalToSupplierConn();

            System.out.println(natToSup.getSourceNode() + " -> " + regToNat.getSourceNode()
                    + " -> " + regToNat.getDestinationNode() + " -> " + locToReg.getDestinationNode());
            return Arrays.asList(road.getNationalToSupplierConn().getSourceNode().getName(), regToNat.getSourceNode().getName(),
                    regToNat.getDestinationNode().getName(), road.getLocalToRegionalConn().getDestinationNode().getName());
        }).collect(Collectors.toList());

        Map<String, String> citiesMap = new HashMap<>();
        result.forEach(road -> {
            Connection localToRegionalConn = road.getLocalToRegionalConn();
            Connection nationalToSupplierConn = road.getNationalToSupplierConn();
            citiesMap.put(localToRegionalConn.getDestinationNode().getName(), NodeType.LOCAL.name());
            citiesMap.put(localToRegionalConn.getSourceNode().getName(), NodeType.REGIONAL.name());
            citiesMap.put(nationalToSupplierConn.getDestinationNode().getName(), NodeType.NATIONAL.name());
            citiesMap.put(nationalToSupplierConn.getSourceNode().getName(), NodeType.SUPPLIER.name());
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
        return calcConnectionPrice(localToRegionalConn);
    }

    private BigDecimal calcConnectionPrice(Connection localToRegionalConn) {
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
            List<BigDecimal> demands = v.stream().map(this::calcConnectionPrice).collect(Collectors.toList());
            BigDecimal minDemand = demands.stream().min(Comparator.naturalOrder()).orElse(null);
            Connection minConnection = v.stream().filter(con -> calcConnectionPrice(con).compareTo(minDemand) == 0).findFirst().orElse(null);
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
                    + localName + "| Total distance is " + totalDistance + "\n");
        });
    }

    private void saveDistanceMapToFile(Map<String, Connection> localToRegionalCheapestConnections,
                                       Map<String, Connection> regionalToNationalClosestConnections,
                                       Map<String, Connection> nationalToSupplierClosestConnections,
                                       String fileName)
    {
        Workbook workbook = new XSSFWorkbook();
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

        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
            workbook.write(fileOut);
        } catch (IOException ignored) { }
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

    private void setTotalDemands(Workbook workbook, String sheetName) {
        Sheet demandSheet = workbook.getSheet(sheetName);

        demandSheet.forEach(row -> {
            if (row.getRowNum() > 1) {
                IntStream.range(0, row.getLastCellNum()).forEach(i -> {
                    String nodeName = row.getCell(0).getStringCellValue();
                    BigDecimal teaDemand = new BigDecimal(getStringValue(row, 1));
                    BigDecimal dryMixesDemand = new BigDecimal(getStringValue(row, 3));
                    BigDecimal sousesDemand = new BigDecimal(getStringValue(row, 5));

                    nodeRepository.getByNameAndNodeType(nodeName, NodeType.LOCAL).ifPresent(node -> {
                        node.setDemand(teaDemand.add(dryMixesDemand).add(sousesDemand));
                        nodeRepository.save(node);
                    });
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

    private List<Connection> parseConnections(Workbook workbook, String sheetName, NodeType senderType, NodeType receiverType) {
        Sheet distanceSheet = workbook.getSheet(sheetName);
        List<Connection> result = new ArrayList<>();
        Row headerRow = distanceSheet.getRow(0);
        Map<Integer, String> headerToIndex = new HashMap<>();

        IntStream.range(1, headerRow.getLastCellNum()).forEach(i -> {
            String nodeName = headerRow.getCell(i).getStringCellValue();
            headerToIndex.put(i, nodeName);
        });

        distanceSheet.forEach(row -> {
            if (row.getRowNum() != 0) {
                IntStream.range(1, row.getLastCellNum()).forEach(i -> {
                    String receiverNodeName = row.getCell(0).getStringCellValue();
                    String senderNodeName = headerToIndex.get(i);

                    Node senderNode = getOrCreateNode(senderNodeName, senderType);
                    Node receiverNode = getOrCreateNode(receiverNodeName, receiverType);

                    nodeRepository.save(senderNode);
                    nodeRepository.save(receiverNode);

                    Connection conn = getOrCreateConnection(senderNode, receiverNode);
                    conn.setDistance(BigDecimal.valueOf(row.getCell(i).getNumericCellValue()));

                    result.add(conn);
                    connectionRepository.save(conn);
                });
            }
        });

        return result;
    }

    private Node getOrCreateNode(String name, NodeType nodeType) {
        Optional<Node> node = nodeRepository.getByNameAndNodeType(name, nodeType);
        return node.orElseGet(() -> new Node(name, nodeType));
    }

    private Connection getOrCreateConnection(Node sender, Node receiver) {
        Optional<Connection> conn = connectionRepository.getBySourceNodeAndDestinationNode(sender, receiver);
        return conn.orElseGet(() -> new Connection(sender, receiver));
    }

    private Map<String, List<Connection>> retrieveConnectionsGroupedByDestination(NodeType senderType, NodeType receiverType) {
        return connectionRepository.findAllBySourceNode_NodeTypeAndDestinationNode_NodeType(senderType, receiverType).stream()
                .collect(Collectors.groupingBy(c -> c.getDestinationNode().getName()));
    }
}
