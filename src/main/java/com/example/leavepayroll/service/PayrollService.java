package com.example.leavepayroll.service;

import com.example.leavepayroll.model.Employee;
import com.example.leavepayroll.model.LeaveRequest;
import com.example.leavepayroll.model.LeaveRequest.LeaveStatus;
import com.example.leavepayroll.model.Payslip;
import com.example.leavepayroll.model.Payslip.PayslipStatus;
import com.example.leavepayroll.repository.EmployeeRepository;
import com.example.leavepayroll.repository.LeaveRequestRepository;
import com.example.leavepayroll.repository.PayslipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PayrollService {

    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Autowired
    public PayrollService(PayslipRepository payslipRepository, EmployeeRepository employeeRepository, LeaveRequestRepository leaveRequestRepository) {
        this.payslipRepository = payslipRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    public List<Payslip> getAllPayslips() {
        return payslipRepository.findAll();
    }

    public List<Payslip> getPayslipsByEmployee(Long employeeId) {
        return payslipRepository.findByEmployeeIdOrderByPayPeriodDesc(employeeId);
    }

    public Optional<Payslip> getPayslipById(Long id) {
        return payslipRepository.findById(id);
    }

    @Transactional
    public Payslip generateMonthlyPayslip(Long employeeId, String payPeriod) {
        // Check if payslip already exists for this period
        Optional<Payslip> existingPayslip = payslipRepository.findByEmployeeIdAndPayPeriod(employeeId, payPeriod);
        if (existingPayslip.isPresent()) {
            throw new IllegalStateException("Payslip for " + payPeriod + " already exists for this employee.");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        Double basicSalary = employee.getBaseSalary();
        
        // Calculate allowances (e.g., 12% of basic salary)
        Double allowances = basicSalary * 0.12;

        // Calculate deductions for approved leave requests with unpaid days in the given period
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(employeeId);
        int totalUnpaidDays = 0;

        for (LeaveRequest leave : leaveRequests) {
            if (leave.getStatus() == LeaveStatus.APPROVED && leave.getUnpaidDays() > 0) {
                // Check if the leave falls within the payPeriod (comparing month and year of start date)
                if (isLeaveInPeriod(leave.getStartDate(), payPeriod)) {
                    totalUnpaidDays += leave.getUnpaidDays();
                }
            }
        }

        // Daily rate based on 30-day month
        Double dailyRate = basicSalary / 30.0;
        Double deductions = dailyRate * totalUnpaidDays;

        // Round to 2 decimal places
        deductions = Math.round(deductions * 100.0) / 100.0;
        allowances = Math.round(allowances * 100.0) / 100.0;

        Double netSalary = basicSalary + allowances - deductions;
        netSalary = Math.max(0.0, Math.round(netSalary * 100.0) / 100.0);

        Payslip payslip = new Payslip(
                employee,
                payPeriod,
                basicSalary,
                allowances,
                deductions,
                netSalary,
                PayslipStatus.PENDING // will default to PENDING until approved/paid
        );

        return payslipRepository.save(payslip);
    }

    @Transactional
    public Payslip markAsPaid(Long payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new IllegalArgumentException("Payslip not found: " + payslipId));
        
        payslip.setStatus(PayslipStatus.PAID);
        return payslipRepository.save(payslip);
    }

    private boolean isLeaveInPeriod(LocalDate date, String payPeriod) {
        if (date == null || payPeriod == null) {
            return false;
        }
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH); // e.g. "July"
        String yearStr = String.valueOf(date.getYear()); // e.g. "2026"
        String datePeriod = monthName + " " + yearStr; // e.g. "July 2026"
        return datePeriod.equalsIgnoreCase(payPeriod.trim());
    }
}
