package com.diploma.app.service;

import com.diploma.app.model.Connection;
import com.diploma.app.model.Node;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMiddle;
import org.chocosolver.solver.search.strategy.selectors.variables.FirstFail;
import org.chocosolver.solver.search.strategy.selectors.variables.Smallest;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelectorWithTies;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

public class Test {
////        Map<String, Connection> resultMap = new HashMap<>();
////        List<BigDecimal> minDemandSum = new ArrayList<>();
//
//    // List<BigDecimal> demands = v.stream().map(con -> con.getDestinationNode().getDemand()
//    //                    .multiply(con.getDistance()).multiply(transportPrice)).collect(Collectors.toList());
//    //            BigDecimal minDemand = demands.stream().min(Comparator.naturalOrder()).orElse(null);
//    //            Connection minConnection = v.stream().filter(con -> con.getDestinationNode().getDemand()
//    //                    .multiply(con.getDistance()).multiply(transportPrice).compareTo(minDemand) == 0).findFirst().orElse(null);
//    //            minDemandSum.add(minDemand);
//    //            resultMap.put(k, minConnection);
//
//
////        resultMap.values().stream().map(Connection::getSourceNode).distinct().forEach(rN -> {
////            BigDecimal demandByRegional = resultMap.values().stream().filter(connection ->
////                    connection.getSourceNode().getName().equals(rN.getName()))
////                    .map(Connection::getDestinationNode)
////                    .map(Node::getDemand).reduce(BigDecimal.ZERO, BigDecimal::add);
////            System.out.println(rN.getName() + " has demand " + demandByRegional);
////        });
//
//    //AtomicInteger i = new AtomicInteger(0);
//    //        resultMap.forEach((k, v) -> System.out.println(i.incrementAndGet() + ". From " + v.getSourceNode().getName() +
//    //                " to " + v.getDestinationNode().getName() + " distance = " + v.getDistance() + ", Min Demand*Distance*TransportPrice = " + k));
//    //        System.out.println("Summed Demand*Distance*TransportPrice = " + minDemandSum.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
//
//    private static Resource location = new ClassPathResource("files/");
//
//    public static void main(String[] args) throws IOException, InvalidFormatException {
//
//        // load parameters
//// number of warehouses
//        int W = 10;
//// number of stores
//        int S = 10;
//// maintenance cost
//        int C = 30;
//// capacity of each warehouse
//        int[] K = new int[]{1, 4, 2, 1, 3};
//// matrix of supply costs, store x warehousescrat
//
//        int[][] P = new int[][]{
//                {20, 24, 11, 25, 30},
//                {28, 27, 82, 83, 74},
//                {74, 97, 71, 96, 70},
//                {2, 55, 73, 69, 61},
//                {46, 96, 59, 83, 4},
//                {42, 22, 29, 67, 59},
//                {1, 5, 73, 59, 56},
//                {10, 73, 13, 43, 96},
//                {93, 35, 63, 85, 46},
//                {47, 65, 55, 71, 95}};
//
//// A new model instance
//        Model model = new Model("WarehouseLocation");
//
// // VARIABLES
//// a warehouse is either open or closed
//        BoolVar[] open = model.boolVarArray("o", W);
//// which warehouse supplies a store
//        IntVar[] supplier = model.intVarArray("supplier", S, 1, W, false);
//// supplying cost per store
//        IntVar[] cost = model.intVarArray("cost", S, 1, 96, true);
//// Total of all costs
//        IntVar tot_cost = model.intVar("tot_cost", 0, 99999, true);
//
//// CONSTRAINTS
//        for (int j = 0; j < S; j++) {
//            // a warehouse is 'open', if it supplies to a store
//            model.element(model.intVar(1), open, supplier[j], 1).post();
//            // Compute 'cost' for each store
//            model.element(cost[j], P[j], supplier[j], 1).post();
//        }
//        for (int i = 0; i < W; i++) {
//            // additional variable 'occ' is created on the fly
//            // its domain includes the constraint on capacity
//            IntVar occ = model.intVar("occur_" + i, 0, K[i], true);
//            // for-loop starts at 0, warehouse index starts at 1
//            // => we count occurrences of (i+1) in 'supplier'
//            model.count(i+1, supplier, occ).post();
//            // redundant link between 'occ' and 'open' for better propagation
//            occ.ge(open[i]).post();
//        }
//// Prepare the constraint that maintains 'tot_cost'
//        int[] coeffs = new int[W + S];
//        Arrays.fill(coeffs, 0, W, C);
//        Arrays.fill(coeffs, W, W + S, 1);
//// then post it
//        model.scalar(ArrayUtils.append(open, cost), coeffs, "=", tot_cost).post();
//
//        model.setObjective(Model.MINIMIZE, tot_cost);
//        Solver solver = model.getSolver();
//        solver.setSearch(Search.intVarSearch(
//                new VariableSelectorWithTies<>(
//                        new FirstFail(model),
//                        new Smallest()),
//                new IntDomainMiddle(false),
//                ArrayUtils.append(supplier, cost, open))
//        );
//        solver.showShortStatistics();
//        Test transportationService = new Test();
//        while(solver.solve()){
//            transportationService.prettyPrint(model, open, W, supplier, S, tot_cost);
//        }
//
////        Workbook workbook = WorkbookFactory.create(location.createRelative("InitData.xlsx").getInputStream());
////        Map<String, List<Connection>> mappedConnections = transportationService.loadInitConnections(workbook);
////        transportationService.setInitPrices(workbook, mappedConnections);
////        transportationService.calc(mappedConnections, 10);
////        transportationService.drawNetwork(workbook);
//    }
//
//
//    private void prettyPrint(Model model, IntVar[] open, int W, IntVar[] supplier, int S, IntVar tot_cost) {
//        StringBuilder st = new StringBuilder();
//        st.append("Solution #").append(model.getSolver().getSolutionCount()).append("\n");
//        for (int i = 0; i < W; i++) {
//            if (open[i].getValue() > 0) {
//                st.append(String.format("\tWarehouse %d supplies customers : ", (i + 1)));
//                for (int j = 0; j < S; j++) {
//                    if (supplier[j].getValue() == (i + 1)) {
//                        st.append(String.format("%d ", (j + 1)));
//                    }
//                }
//                st.append("\n");
//            }
//        }
//        st.append("\tTotal C: ").append(tot_cost.getValue());
//        System.out.println(st.toString());
//    }
//
//
//
//    private void drawNetwork(Workbook workbook) {
//        Sheet distance = workbook.getSheet("Distance");
//        List<String> cities = new ArrayList<>();
//        distance.forEach(row -> {
//            String cityName = row.getCell(0).getStringCellValue();
//            cities.add(cityName);
//        });
//
//        System.out.println(cities.size());
//        HashSet<String> unique = new HashSet<>();
//        HashSet<String> duplicates = new HashSet<>();
//
//        cities.stream().forEach(c -> {
//            if (unique.contains(c)) {
//                duplicates.add(c);
//            } else {
//                unique.add(c);
//            }
//        });
//        System.out.println(duplicates);
//        System.out.println(duplicates.size());
//        System.out.println(unique.size());
//    }
//
//    private void setInitPrices(Workbook workbook, Map<String, List<Connection>> connections) {
//        Sheet demandSheet = workbook.getSheet("Demand");
//
//        demandSheet.forEach(row -> {
//            if (row.getRowNum() > 1) {
//                IntStream.range(0, row.getLastCellNum()).forEach(i -> {
//                    String nodeName = row.getCell(0).getStringCellValue();
//                    BigDecimal teaDemand = new BigDecimal(getStringValue(row, 1));
//                    BigDecimal dryMixesDemand = new BigDecimal(getStringValue(row, 3));
//                    BigDecimal sousesDemand = new BigDecimal(getStringValue(row, 5));
//
//                    Optional.ofNullable(connections.get(nodeName)).ifPresent(cons -> cons
//                            .forEach(con -> con.getDestinationNode().setDemand(teaDemand.add(dryMixesDemand).add(sousesDemand))));
//                });
//            }
//        });
//
//        System.out.println();
//    }
//
//    private static String getStringValue(final Row row, final int index) {
//        Cell cell = row.getCell(index);
//        if (cell != null) {
//
//            try {
//                if (DateUtil.isCellDateFormatted(cell)) {
//                    return new DataFormatter().formatCellValue(cell);
//                }
//                return String.valueOf(cell.getNumericCellValue());
//            } catch (IllegalStateException e) {
//                return cell.getStringCellValue();
//            }
//        }
//        return null;
//    }
//
//    private Map<String, List<Connection>> loadInitConnections(Workbook workbook) throws IOException, InvalidFormatException {
//        Sheet distanceSheet = workbook.getSheet("Distance");
//        List<Connection> connections = new ArrayList<>();
//        Row headerRow = distanceSheet.getRow(0);
//        Map<Integer, String> headerToIndex = new HashMap<>();
//
//        IntStream.range(1, headerRow.getLastCellNum()).forEach(i -> {
//            String nodeName = headerRow.getCell(i).getStringCellValue();
//            headerToIndex.put(i, nodeName);
//        });
//
//        distanceSheet.forEach(row -> {
//            if (row.getRowNum() != 0) {
//                IntStream.range(1, row.getLastCellNum()).forEach(i -> {
//                    String localNodeName = row.getCell(0).getStringCellValue();
//                    String regionalNodeName = headerToIndex.get(i);
//                    if (connections.stream().noneMatch(c -> c.getDestinationNode().getName().equals(localNodeName)
//                            && c.getSourceNode().getName().equals(regionalNodeName))) {
//                        connections.add(new Connection(new Node(regionalNodeName), new Node(localNodeName),
//                                new BigDecimal(row.getCell(i).getNumericCellValue())));
//                    }
//                });
//            }
//        });
//
//        return connections.stream().collect(Collectors.groupingBy(c -> c.getDestinationNode().getName()));
//    }
//
//    public void calc(Map<String, List<Connection>> mappedConnections, Integer regionalWhAmount) throws LpSolveException {
//        BigDecimal transportPrice = BigDecimal.valueOf(0.678);
//        System.out.println(mappedConnections.size() + " Local Cities");
////        try {
//        // Create a problem with 4 variables and 0 constraints
//        LpSolve solver = LpSolve.makeLp(0, regionalWhAmount);
//        StringBuilder objFunc = new StringBuilder();
//
//
//
//        mappedConnections.forEach((k, v) -> {
//
//            objFunc.append(v.stream().map(con -> con.getDestinationNode().getDemand()
//                    .multiply(con.getDistance()).multiply(transportPrice))
//                    .map(BigDecimal::toString)
//                    .collect(joining(" ")));
//            BigDecimal demandSum = v.stream().map(Connection::getDestinationNode).map(Node::getDemand).reduce(BigDecimal.ZERO, BigDecimal::add);
//            StringBuilder sumConstraint = new StringBuilder(v.stream().map(con -> "1").collect(joining(" ")));
//            IntStream.range(0, regionalWhAmount - v.size()).forEach(i -> sumConstraint.append(" 0"));
//
////            IntStream.range(0, v.size()).forEach(i -> {
////                List<String> constr = new LinkedList<>(new ArrayList<>(v.size()));
////
////                IntStream.range(0, v.size()).forEach(j -> {
////                    if (j == i) {
////                        constr.add(i, "1");
////                    } else {
////                        constr.add(j, "0");
////                    }
////                });
////
////                String constraint = String.join(" ", constr);
////                try {
////                    solver.strAddConstraint(constraint, LpSolve.LE, 1);
////                    solver.strAddConstraint(constraint, LpSolve.GE, 0);
////                } catch (LpSolveException e) {
////                    e.printStackTrace();
////                }
////
////            });
//
//
////            try {
////                solver.strAddConstraint(sumConstraint.toString(), LpSolve.EQ, 1);
//////                    solver.strAddConstraint(sumConstraint.toString(), LpSolve.EQ, demandSum.doubleValue());
////            } catch (LpSolveException e) {
////                e.printStackTrace();
////            }
//        });
////        Map<Node, List<Node>> regionalToLocal = resultMap.entrySet().stream().collect(Collectors
////                .toMap(e -> e.getValue().getSourceNode(),
////                        e -> {
////                            List<Node> collect = resultMap.values().stream()
////                                    .filter(con -> con.getSourceNode().getName()
////                                            .equals(e.getValue().getSourceNode().getName())).map(Connection::getDestinationNode)
////                                    .collect(Collectors.toList());
////                            return collect;
////                        }));
////
////        regionalToLocal.forEach((k, v) -> {
////            System.out.println(k.getName() + " demand is " + v.stream()
////                    .map(Node::getDemand).reduce(BigDecimal.ZERO, BigDecimal::add));
////        });
//        //            solver.strAddConstraint("1 0 0 0 0 0 0 0 0 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 1 0 0 0 0 0 0 0 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 0 1 0 0 0 0 0 0 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 0 0 1 0 0 0 0 0 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 0 0 0 1 0 0 0 0 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 0 0 0 0 1 0 0 0 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 0 0 0 0 0 1 0 0 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 0 0 0 0 0 0 1 0 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 0 0 0 0 0 0 0 1 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 0 0 0 0 0 0 0 0 1", LpSolve.GE, 0);
////
////            solver.strAddConstraint("1 0 0 0 0 0 0 0 0 0", LpSolve.LE, 1);
////            solver.strAddConstraint("0 1 0 0 0 0 0 0 0 0", LpSolve.LE, 1);
////            solver.strAddConstraint("0 0 1 0 0 0 0 0 0 0", LpSolve.LE, 1);
////            solver.strAddConstraint("0 0 0 1 0 0 0 0 0 0", LpSolve.LE, 1);
////            solver.strAddConstraint("0 0 0 0 1 0 0 0 0 0", LpSolve.LE, 1);
////            solver.strAddConstraint("0 0 0 0 0 1 0 0 0 0", LpSolve.LE, 1);
////            solver.strAddConstraint("0 0 0 0 0 0 1 0 0 0", LpSolve.LE, 1);
////            solver.strAddConstraint("0 0 0 0 0 0 0 1 0 0", LpSolve.LE, 1);
////            solver.strAddConstraint("0 0 0 0 0 0 0 0 1 0", LpSolve.LE, 1);
////            solver.strAddConstraint("0 0 0 0 0 0 0 0 0 1", LpSolve.LE, 1);
//
//
////            String collect = mappedConnections.entrySet().stream().map(e -> "1").collect(joining(" "));
//////            System.out.println(collect.split(" ").length);
////            try {
////                solver.strAddConstraint(collect, LpSolve.LE, 10);
////                solver.strAddConstraint(collect, LpSolve.GE, 1);
////            } catch (LpSolveException e) {
////                e.printStackTrace();
////            }
//            solver.strSetObjFn(objFunc.toString());
//        IntStream.range(0, regionalWhAmount).forEach(i -> {
//            try {
////                solver.setLowbo(i, 0);
//                solver.setUpbo(i, 1);
//            } catch (LpSolveException e) {
//                e.printStackTrace();
//            }
//
//        });
////         solve the problem
//            solver.solve();
//
////         print solution
//            System.out.println("Value of objective function: " + (int) Math.round(solver.getObjective()));
//
//
//            double[] ptrVariables = solver.getPtrVariables();
//            IntStream.range(0, ptrVariables.length).forEach(i ->
//                    System.out.println("Value of var[" + (i + 1) + "] = " + (int) Math.round(ptrVariables[i])));
//
//            // delete the problem and free memory
//            solver.deleteLp();
//
////            // add constraints
////            solver.strAddConstraint("1 1 0 1 0", LpSolve.GE, 1);
////            solver.strAddConstraint("0 1 1 1 0", LpSolve.GE, 1);
////            solver.strAddConstraint("1 1 0 1 0", LpSolve.GE, 2);
////            solver.strAddConstraint("1 1 1 1 0", LpSolve.GE, 1);
////            solver.strAddConstraint("1 1 0 1 1", LpSolve.GE, 3);
////            solver.strAddConstraint("0 1 1 1 0", LpSolve.GE, 1);
////            solver.strAddConstraint("1 0 1 1 0", LpSolve.GE, 1);
////            solver.strAddConstraint("1 0 1 1 1", LpSolve.GE, 3);
////            solver.strAddConstraint("0 1 1 1 0", LpSolve.GE, 2);
////            solver.strAddConstraint("0 1 1 1 0", LpSolve.GE, 1);
////
////            solver.strAddConstraint("1 0 0 0 0", LpSolve.GE, 0);
////            solver.strAddConstraint("1 0 0 0 0", LpSolve.LE, 1);
////
////            solver.strAddConstraint("0 1 0 0 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 1 0 0 0", LpSolve.LE, 1);
////
////            solver.strAddConstraint("0 0 1 0 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 0 1 0 0", LpSolve.LE, 1);
////
////            solver.strAddConstraint("0 0 0 1 0", LpSolve.GE, 0);
////            solver.strAddConstraint("0 0 0 1 0", LpSolve.LE, 1);
////
////            solver.strAddConstraint("0 0 0 0 1", LpSolve.GE, 0);
////            solver.strAddConstraint("0 0 0 0 1", LpSolve.LE, 1);
////
////            // set objective function
////            solver.strSetObjFn("3 2 4 1 6");
////
////            // solve the problem
////            solver.solve();
////
////            // print solution
////            System.out.println("Value of objective function: " + (int) Math.round(solver.getObjective()));
////
////            double[] ptrVariables = solver.getPtrVariables();
////            IntStream.range(0, ptrVariables.length).forEach(i ->
////                    System.out.println("Value of var[" + (i + 1) + "] = " + (int) Math.round(ptrVariables[i])));
////
////            // delete the problem and free memory
////            solver.deleteLp();
////        } catch (LpSolveException e) {
////            e.printStackTrace();
////        }
//    }
}
