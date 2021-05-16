//package com.diploma.app;
//
//import lpsolve.LpSolve;
//import lpsolve.LpSolveException;
//import org.chocosolver.solver.Model;
//import org.chocosolver.solver.variables.IntVar;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.stream.IntStream;
//
//public class AppApplicationTests {
//
//    @Test
//    public void contextLoads() throws LpSolveException {
//
//        LpSolve solver = LpSolve.makeLp(0, 3);
//
//        solver.strAddConstraint("432 231 324", LpSolve.GE, 1);
//        solver.strAddConstraint("223 533 3", LpSolve.GE, 1);
//        solver.strAddConstraint("3 1223 12", LpSolve.GE, 1);
//
////        solver.setLowbo(1, 1);
////        solver.setLowbo(2, 1);
////        solver.setLowbo(3, 1);
//        solver.setPivoting(1);
//
//        solver.strSetObjFn("1 1 1");
//        solver.setMinim();
//        solver.solve();
//
//        System.out.println("Value of objective function: " + solver.getObjective());
//        double[] var = solver.getPtrVariables();
//        for (int i = 0; i < var.length; i++) {
//            System.out.println("Value of var[" + i + "] = " + var[i]);
//        }
//
//        // delete the problem and free memory
//        solver.deleteLp();
//    }
//
//}
