package com.example.leavepayroll.controller;

import com.example.leavepayroll.model.Employee;
import com.example.leavepayroll.model.LeaveRequest;
import com.example.leavepayroll.model.Payslip;
import com.example.leavepayroll.service.EmployeeService;
import com.example.leavepayroll.service.LeaveService;
import com.example.leavepayroll.service.PayrollService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final EmployeeService employeeService;
    private final LeaveService leaveService;
    private final PayrollService payrollService;

    @Autowired
    public AdminController(EmployeeService employeeService, LeaveService leaveService, PayrollService payrollService) {
        this.employeeService = employeeService;
        this.leaveService = leaveService;
        this.payrollService = payrollService;
    }

    @GetMapping
    public String adminPanel(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (role == null) {
            return "redirect:/login";
        }
        
        // Block non-admin employees from access
        if (!"ADMIN".equals(role)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access Denied: You do not have administrator permissions.");
            return "redirect:/";
        }

        List<Employee> employees = employeeService.getAllEmployees();
        List<LeaveRequest> leaveRequests = leaveService.getAllLeaveRequests();
        List<Payslip> payslips = payrollService.getAllPayslips();

        long totalEmployees = employees.size();
        
        LocalDate today = LocalDate.now();
        List<LeaveRequest> leavesToday = leaveRequests.stream()
                .filter(lr -> lr.getStatus() == LeaveRequest.LeaveStatus.APPROVED)
                .filter(lr -> !today.isBefore(lr.getStartDate()) && !today.isAfter(lr.getEndDate()))
                .toList();

        long employeesOnLeaveToday = leavesToday.stream()
                .map(lr -> lr.getEmployee().getId())
                .distinct()
                .count();

        double totalMonthlyPayroll = employees.stream()
                .mapToDouble(Employee::getBaseSalary)
                .sum();

        model.addAttribute("employees", employees);
        model.addAttribute("leaveRequests", leaveRequests);
        model.addAttribute("payslips", payslips);
        
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("employeesOnLeaveToday", employeesOnLeaveToday);
        model.addAttribute("leavesToday", leavesToday);
        model.addAttribute("totalMonthlyPayroll", totalMonthlyPayroll);
        
        model.addAttribute("sessionName", session.getAttribute("name"));
        model.addAttribute("sessionRole", role);

        return "admin";
    }

    @PostMapping("/employees/add")
    public String registerEmployee(HttpSession session,
                                   @RequestParam("name") String name,
                                   @RequestParam("email") String email,
                                   @RequestParam("department") String department,
                                   @RequestParam("designation") String designation,
                                   @RequestParam("baseSalary") Double baseSalary,
                                   RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        try {
            Employee employee = new Employee(name, email, department, designation, baseSalary, 15);
            employeeService.saveEmployee(employee);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully registered new employee: " + name + " with base salary: $" + baseSalary);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error registering employee: " + e.getMessage());
        }

        return "redirect:/admin";
    }

    @PostMapping("/leaves/{id}/approve")
    public String approveLeave(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        try {
            LeaveRequest approved = leaveService.approveLeaveRequest(id);
            String msg = "Leave request for " + approved.getEmployee().getName() + " approved.";
            if (approved.getUnpaidDays() > 0) {
                msg += " Note: " + approved.getUnpaidDays() + " day(s) will be treated as Loss of Pay (LOP) since remaining balance was exceeded.";
            }
            redirectAttributes.addFlashAttribute("successMessage", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error approving leave: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/leaves/{id}/reject")
    public String rejectLeave(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        try {
            LeaveRequest rejected = leaveService.rejectLeaveRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", "Leave request for " + rejected.getEmployee().getName() + " has been rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error rejecting leave: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/payroll/generate")
    public String generatePayroll(@RequestParam("employeeId") Long employeeId,
                                  @RequestParam("payPeriod") String payPeriod,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        try {
            Payslip payslip = payrollService.generateMonthlyPayslip(employeeId, payPeriod);
            redirectAttributes.addFlashAttribute("successMessage", "Payslip generated successfully for " + 
                    payslip.getEmployee().getName() + " for period: " + payPeriod);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error generating payslip: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/payslip/{id}/pay")
    public String markAsPaid(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        try {
            Payslip payslip = payrollService.markAsPaid(id);
            redirectAttributes.addFlashAttribute("successMessage", "Payslip for " + 
                    payslip.getEmployee().getName() + " marked as PAID.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating payslip status: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/employees/{id}/delete")
    public String deleteEmployee(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        try {
            Employee employee = employeeService.getEmployeeById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
            employeeService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully deleted employee: " + employee.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting employee: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/employees/{id}/edit")
    public String editEmployee(@PathVariable("id") Long id,
                               @RequestParam("name") String name,
                               @RequestParam("email") String email,
                               @RequestParam("department") String department,
                               @RequestParam("designation") String designation,
                               @RequestParam("baseSalary") Double baseSalary,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        try {
            Employee employee = employeeService.getEmployeeById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
            employee.setName(name);
            employee.setEmail(email);
            employee.setDepartment(department);
            employee.setDesignation(designation);
            employee.setBaseSalary(baseSalary);
            employeeService.saveEmployee(employee);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully updated employee: " + name);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating employee: " + e.getMessage());
        }
        return "redirect:/admin";
    }
}
