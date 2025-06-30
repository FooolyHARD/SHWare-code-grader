package ru.senechka.syntaxtreemodule

import org.springframework.boot.autoconfigure.SpringBootApplication
import ru.senechka.syntaxtreemodule.util.AnalyzerRunner


@SpringBootApplication
class SyntaxTreeModuleApplication

fun main(args: Array<String>) {

    val javaCode = """
        public class BadSingleton {
            public static BadSingleton instance = new BadSingleton();
            public BadSingleton() {}
            public static BadSingleton getInstance() {
                return instance;
            }
        }

        public class Person {
            public String name;
            protected int age;
            String address;
        }
    """.trimIndent()

    val enabledAnalyses = listOf("singleton", "solid", "access", "isp", "metrics")

    val resultJson = AnalyzerRunner.analyze(javaCode, enabledAnalyses)

    println(resultJson)

}

