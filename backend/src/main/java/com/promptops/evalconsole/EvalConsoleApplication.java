package com.promptops.evalconsole;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.promptops.evalconsole.persistence.mapper")
public class EvalConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvalConsoleApplication.class, args);
    }
}
