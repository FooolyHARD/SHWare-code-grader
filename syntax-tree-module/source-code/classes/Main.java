package com.example;

import java.io.*;

public class Main {
    // 1. Бессмысленное сравнение (EQ_CHECK)
    public void badEquals() {
        int x = 10;
        if (x == x) {  // SpotBugs найдёт
            System.out.println("WTF?");
        }
    }

    // 2. Риск NPE (NP_NULL)
    public void potentialNPE(String str) {
        System.out.println(str.length());  // SpotBugs: NP_NULL_ON_SOME_PATH
    }

    // 3. Незакрытый поток (OS_OPEN_STREAM)
    public void leakyStream() throws IOException {
        FileOutputStream out = new FileOutputStream("temp.txt");  // SpotBugs найдёт
        out.write("oops".getBytes());
    }
}