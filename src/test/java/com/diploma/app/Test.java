package com.diploma.app;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {

    @org.junit.Test
    public void main() {

        List<Cool> objs = Arrays.asList(new Cool("A"), new Cool("B"), new Cool("C"));
        List<List<Cool>> lists = printCombination(objs, objs.size(), 2);

        System.out.println(lists);

//        int arr[] = {1, 2, 3, 4, 5};
//        int r = 3;
//        int n = arr.length;
//        printCombination(arr, n, r);
//        char str[] = {'A', 'B', 'C', 'A'};
//        int n = str.length;
//        findPermutations(str, 0, n);
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private class Cool {
        private String s;

        @Override
        public String toString() {
            return s;
        }
    }


    /* arr[]  ---> Input Array
      data[] ---> Temporary array to store current combination
      start & end ---> Staring and Ending indexes in arr[]
      index  ---> Current index in data[]
      r ---> Size of a combination to be printed */
    static void combinationUtil(List<Cool> arr, Cool data[], int start,
                                int end, int index, int r, List<List<Cool>> result) {
        // Current combination is ready to be printed, print it
        if (index == r) {
            List<Cool> temp = new ArrayList<>();
            for (int j = 0; j < r; j++) {
                temp.add(data[j]);
                System.out.print(data[j] + " ");
            }
            result.add(temp);
            System.out.println("");
            return;
        }

        // replace index with all possible elements. The condition
        // "end-i+1 >= r-index" makes sure that including one element
        // at index will make a combination with remaining elements
        // at remaining positions
        for (int i = start; i <= end && end - i + 1 >= r - index; i++) {
            data[index] = arr.get(i);
            combinationUtil(arr, data, i + 1, end, index + 1, r, result);
        }
    }

    // The main function that prints all combinations of size r
    // in arr[] of size n. This function mainly uses combinationUtil()
    static List<List<Cool>> printCombination(List<Cool> objs, int n, int r) {
        // A temporary array to store all combination one by one
        Cool data[] = new Cool[r];

        // Print all combination using temprary array 'data[]'
        List<List<Cool>> result = new ArrayList<>();
        combinationUtil(objs, data, 0, n - 1, 0, r, result);
        return result;
    }
}
