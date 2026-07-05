package com.example.leavepayroll.service;

import com.example.leavepayroll.model.Employee;
import com.example.leavepayroll.model.LeaveRequest;
import com.example.leavepayroll.model.LeaveRequest.LeaveStatus;
import com.example.leavepayroll.repository.EmployeeRepository;
import com.example.leavepayroll.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public LeaveService(LeaveRequestRepository leaveRequestRepository, EmployeeRepository employeeRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequestRepository.findAll();
    }

    public List<LeaveRequest> getLeaveRequestsByEmployee(Long employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(employeeId);
    }

    public Optional<LeaveRequest> getLeaveRequestById(Long id) {
        return leaveRequestRepository.findById(id);
    }

    public LeaveRequest submitLeaveRequest(LeaveRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        // Calculate total days including start and end date
        int days = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        request.setTotalDays(days);
        request.setStatus(LeaveStatus.PENDING);
        request.setUnpaidDays(0); // Set to 0 initially, will be calculated on approval

        return leaveRequestRepository.save(request);
    }

    @Transactional
    public LeaveRequest approveLeaveRequest(Long id) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + id));

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Leave request is not in PENDING status.");
        }

        Employee employee = request.getEmployee();
        int requestedDays = request.getTotalDays();
        int remaining = employee.getRemainingLeaves();

        if (requestedDays <= remaining) {
            // Employee has enough leaves
            employee.setRemainingLeaves(remaining - requestedDays);
            request.setUnpaidDays(0);
        } else {
            // Employee doesn't have enough leaves, portion becomes Loss of Pay (LOP)
            int paidDays = remaining;
            int unpaidDays = requestedDays - paidDays;
            
            employee.setRemainingLeaves(0);
            request.setUnpaidDays(unpaidDays);
        }

        request.setStatus(LeaveStatus.APPROVED);
        
        employeeRepository.save(employee);
        return leaveRequestRepository.save(request);
    }

    @Transactional
    public LeaveRequest rejectLeaveRequest(Long id) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + id));

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Leave request is not in PENDING status.");
        }

        request.setStatus(LeaveStatus.REJECTED);
        request.setUnpaidDays(0);

        return leaveRequestRepository.save(request);
    }
}
