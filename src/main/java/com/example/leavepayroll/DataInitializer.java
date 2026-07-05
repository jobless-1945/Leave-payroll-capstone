package com.example.leavepayroll;

import com.example.leavepayroll.model.Employee;
import com.example.leavepayroll.model.LeaveRequest;
import com.example.leavepayroll.model.LeaveRequest.LeaveStatus;
import com.example.leavepayroll.model.LeaveRequest.LeaveType;
import com.example.leavepayroll.model.Payslip;
import com.example.leavepayroll.model.Payslip.PayslipStatus;
import com.example.leavepayroll.repository.EmployeeRepository;
import com.example.leavepayroll.repository.LeaveRequestRepository;
import com.example.leavepayroll.repository.PayslipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayslipRepository payslipRepository;

    @Autowired
    public DataInitializer(EmployeeRepository employeeRepository,
                           LeaveRequestRepository leaveRequestRepository,
                           PayslipRepository payslipRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.payslipRepository = payslipRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seeding database only if it's empty
        if (employeeRepository.count() == 0) {
            System.out.println("Initializing sample data for Leave & Payroll Portal...");

            // Create Employees
            Employee emp1 = new Employee("John Doe", "john.doe@example.com", "Engineering", "Senior Developer", 6000.0, 15);
            Employee emp2 = new Employee("Jane Smith", "jane.smith@example.com", "Design", "UI/UX Lead", 5500.0, 15);
            Employee emp3 = new Employee("Bob Johnson", "bob.johnson@example.com", "Operations", "Operations Manager", 5000.0, 15);

            employeeRepository.save(emp1);
            employeeRepository.save(emp2);
            employeeRepository.save(emp3);

            // Create some past approved leaves for John Doe (remaining leaves will be updated)
            LeaveRequest leave1 = new LeaveRequest(emp1, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12), 3, LeaveType.CASUAL, "Family function");
            leave1.setStatus(LeaveStatus.APPROVED);
            leaveRequestRepository.save(leave1);
            emp1.setRemainingLeaves(emp1.getRemainingLeaves() - 3);

            // Create a sick leave for John Doe
            LeaveRequest leave2 = new LeaveRequest(emp1, LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 23), 2, LeaveType.SICK, "Medical recovery");
            leave2.setStatus(LeaveStatus.APPROVED);
            leaveRequestRepository.save(leave2);
            emp1.setRemainingLeaves(emp1.getRemainingLeaves() - 2);
            
            // Create a pending leave request for John Doe
            LeaveRequest leave3 = new LeaveRequest(emp1, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 17), 3, LeaveType.ANNUAL, "Summer trip");
            leaveRequestRepository.save(leave3);

            // Create a leave request that will exceed Jane Smith's balance to demonstrate unpaid leave
            LeaveRequest leave4 = new LeaveRequest(emp2, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 18), 18, LeaveType.ANNUAL, "Long vacation");
            leave4.setStatus(LeaveStatus.APPROVED);
            leave4.setUnpaidDays(3); // 18 requested - 15 remaining = 3 unpaid LOP days
            leaveRequestRepository.save(leave4);
            emp2.setRemainingLeaves(0);

            employeeRepository.save(emp1);
            employeeRepository.save(emp2);

            // Create Payslips
            // John Doe June Payslip (no deductions)
            Payslip payslip1 = new Payslip(emp1, "June 2026", 6000.0, 720.0, 0.0, 6720.0, PayslipStatus.PAID);
            payslipRepository.save(payslip1);

            // Jane Smith June Payslip (with 3 LOP days deduction)
            // Daily rate = 5500 / 30 = 183.33. LOP Deduction = 183.33 * 3 = 550.0. Allowance = 5500 * 0.12 = 660.0. Net = 5500 + 660 - 550 = 5610.0
            Payslip payslip2 = new Payslip(emp2, "June 2026", 5500.0, 660.0, 550.0, 5610.0, PayslipStatus.PENDING);
            payslipRepository.save(payslip2);

            System.out.println("Sample data initialization complete.");
        }
    }
}
