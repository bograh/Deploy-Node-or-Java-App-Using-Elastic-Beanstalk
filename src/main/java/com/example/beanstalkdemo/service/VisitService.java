package com.example.beanstalkdemo.service;

import com.example.beanstalkdemo.model.Visit;
import com.example.beanstalkdemo.repository.VisitRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisitService {

    private final VisitRepository repository;

    public VisitService(VisitRepository repository) {
        this.repository = repository;
    }

    public Visit save(String path, String message) {
        return repository.save(new Visit(path, message));
    }

    public List<Visit> findAll() {
        return repository.findAll();
    }
}
