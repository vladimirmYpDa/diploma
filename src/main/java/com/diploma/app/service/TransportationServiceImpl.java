package com.diploma.app.service;

import com.diploma.app.model.CitiesRoadsDto;
import com.diploma.app.model.Connection;
import com.diploma.app.model.Node;
import com.diploma.app.model.Road;
import com.diploma.app.repository.ConnectionRepository;
import com.diploma.app.repository.NodeRepository;
import com.diploma.app.repository.RoadRepository;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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

    private static Resource location = new ClassPathResource("files/");
    private static final BigDecimal TRANSPORT_PRICE = BigDecimal.valueOf(0.678);

    @Autowired
    public TransportationServiceImpl(RoadRepository roadRepository, ConnectionRepository connectionRepository, NodeRepository nodeRepository) {
        this.roadRepository = roadRepository;
        this.connectionRepository = connectionRepository;
        this.nodeRepository = nodeRepository;
    }

    public CitiesRoadsDto process(InputStream inputStream) throws IOException, InvalidFormatException {
        Workbook workbook = WorkbookFactory.create(inputStream);

        Map<String, List<Connection>> regionalToLocalConnections = loadInitConnections(workbook,
                "RegionalToLocalDistance");
        Map<String, List<Connection>> regionalToNationalConnections = loadInitConnections(workbook,
                "NationalToRegionalDistance");
        Map<String, List<Connection>> nationalToSupplierConnections = loadInitConnections(workbook,
                "SuppliersToNationalDistance");

        setInitPrices(workbook, regionalToLocalConnections);
        Map<String, Connection> localToRegionalCheapestConnections = getCheapestConnections(regionalToLocalConnections);
        Map<String, Connection> regionalToNationalClosestConnections = getClosestConnections(regionalToNationalConnections);
        Map<String, Connection> nationalToSupplierClosestConnections = getClosestConnections(nationalToSupplierConnections);

        printSolution(localToRegionalCheapestConnections,
                regionalToNationalClosestConnections,
                nationalToSupplierClosestConnections);

        List<Road> roads = saveData(localToRegionalCheapestConnections,
                regionalToNationalClosestConnections,
                nationalToSupplierClosestConnections);

        Map<String, String> citiesMap = new HashMap<>();
        roads.forEach(road -> {
            Connection localToRegionalConn = road.getLocalToRegionalConn();
            Connection nationalToSupplierConn = road.getNationalToSupplierConn();
            citiesMap.put(localToRegionalConn.getDestinationNode().getName(), LOCAL);
            citiesMap.put(localToRegionalConn.getSourceNode().getName(), REGIONAL);
            citiesMap.put(nationalToSupplierConn.getDestinationNode().getName(), NATIONAL);
            citiesMap.put(nationalToSupplierConn.getSourceNode().getName(), SUPPLIER);
        });

        return new CitiesRoadsDto(citiesMap, roads);
//        transportationService.saveSolutionToFile(localToRegionalCheapestConnections,
//                regionalToNationalClosestConnections, nationalToSupplierClosestConnections, workbook);
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

    @Transactional
    public List<Road> saveData(Map<String, Connection> localToRegionalCheapestConnections,
                               Map<String, Connection> regionalToNationalClosestConnections,
                               Map<String, Connection> nationalToSupplierClosestConnections) {
        List<Road> roads = new ArrayList<>();
        regionalToNationalClosestConnections.values().forEach(connectionRepository::save);
        nationalToSupplierClosestConnections.values().forEach(connectionRepository::save);

        localToRegionalCheapestConnections.forEach((localName, localToRegionalConn) -> {
            Node localSourceNode = localToRegionalConn.getSourceNode();
            Connection regionalToNationalConn = regionalToNationalClosestConnections.get(localSourceNode.getName());
            Node regionalSourceNode = regionalToNationalConn.getSourceNode();
            Connection nationalToSupplierConn = nationalToSupplierClosestConnections.get(regionalSourceNode.getName());

            BigDecimal nationalToSupplierDistance = nationalToSupplierConn.getDistance();
            BigDecimal regionalToNationalDistance = regionalToNationalConn.getDistance();
            BigDecimal localToRegionalDistance = localToRegionalConn.getDistance();
            BigDecimal totalDistance = nationalToSupplierDistance.add(regionalToNationalDistance).add(localToRegionalDistance);

            Road road = new Road(totalDistance, localToRegionalConn, regionalToNationalConn, nationalToSupplierConn);
            connectionRepository.save(localToRegionalConn);
            roadRepository.save(road);
            roads.add(road);
        });

        System.out.println("Stored successfully!");
        return roads;
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

    private Map<String, Connection> getClosestConnections(Map<String, List<Connection>> mappedConnections) {
        System.out.println(mappedConnections.size() + " Local Cities");
        Map<String, Connection> resultMap = new HashMap<>();

        mappedConnections.forEach((k, v) -> resultMap.put(k, v.stream().min(Comparator.comparingDouble(connection ->
                connection.getDistance().doubleValue())).orElse(null)));

        return resultMap;
    }

    private Map<String, Connection> getCheapestConnections(Map<String, List<Connection>> mappedConnections) {
        System.out.println(mappedConnections.size() + " Local Cities");
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
