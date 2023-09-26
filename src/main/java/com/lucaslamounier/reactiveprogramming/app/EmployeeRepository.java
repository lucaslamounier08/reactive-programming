package com.lucaslamounier.reactiveprogramming.app;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EmployeeRepository {

    public Mono<Employee> findEmployeeById(String id) {
        return Mono.just(createEmployee(id));
    }

    public Flux<Employee> findAllEmployees() {
        return Flux.just(createEmployee("123"), createEmployee("12345"));
    }

    private static Employee createEmployee(String id) {
        return Employee.builder().id(id).build();
    }
}


