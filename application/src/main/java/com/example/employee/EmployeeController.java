package com.example.employee;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final AtomicLong ids = new AtomicLong(3);
    private final List<Employee> employees = new CopyOnWriteArrayList<>(List.of(
        new Employee(1L, "Ava Patel", "Platform Engineer", "Engineering"),
        new Employee(2L, "Noah Kim", "QA Engineer", "Quality"),
        new Employee(3L, "Mia Johnson", "Product Manager", "Product")
    ));

    @GetMapping
    public List<Employee> all() { return employees; }

    @GetMapping("/{id}")
    public Employee byId(@PathVariable Long id) {
        return employees.stream().filter(e -> e.id().equals(id)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Employee create(@Valid @RequestBody CreateEmployee request) {
        Employee employee = new Employee(ids.incrementAndGet(), request.name(), request.role(), request.department());
        employees.add(employee);
        return employee;
    }

    public record CreateEmployee(@NotBlank String name, @NotBlank String role, @NotBlank String department) {}
}
