package com.lucaslamounier.reactiveprogramming.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
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

    // shows how subscribe works asynchronously
    public void testFlux() {
        log.info("First method line");

        WebClient client = WebClient.create("http://localhost:8080");
        Mono<Employee> employeeMono = client.get()
                .uri("/employees/{id}", "teste")
                .retrieve()
                .bodyToMono(Employee.class);

        employeeMono.subscribe(employee -> log.info("MONO: This should be executed after the last method line. {}", employee));

        Flux<Employee> employeeFlux = client.get()
                .uri("/employees")
                .retrieve()
                .bodyToFlux(Employee.class);

        employeeFlux.subscribe(employee -> log.info("FLUX: This should be executed after the last method line. {}", employee));

        log.info("Last method line");
    }
}


