package com.example.leavepayroll.controller;

import com.example.leavepayroll.model.Employee;
import com.example.leavepayroll.model.LeaveRequest;
import com.example.leavepayroll.model.LeaveRequest.LeaveType;
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
@RequestMapping("/")
public class PortalController {

    private final EmployeeService employeeService;
    private final LeaveService leaveService;
    private final PayrollService payrollService;

    @Autowired
    public PortalController(EmployeeService employeeService, LeaveService leaveService, PayrollService payrollService) {
        this.employeeService = employeeService;
        this.leaveService = leaveService;
        this.payrollService = payrollService;
    }

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if (role == null) {
            return "redirect:/login";
        }
        
        // Redirect admins directly to their dashboard
        if ("ADMIN".equals(role)) {
            return "redirect:/admin";
        }

        Long employeeId = (Long) session.getAttribute("employeeId");
        Employee employee = employeeService.getEmployeeById(employeeId).orElse(null);
        
        if (employee != null) {
            model.addAttribute("selectedEmployee", employee);
            model.addAttribute("leaveRequests", leaveService.getLeaveRequestsByEmployee(employeeId));
            model.addAttribute("payslips", payrollService.getPayslipsByEmployee(employeeId));
        }

        model.addAttribute("sessionName", session.getAttribute("name"));
        model.addAttribute("sessionRole", role);
        return "dashboard";
    }

    @GetMapping("/leaves")
    public String leavesPage(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if (role == null) {
            return "redirect:/login";
        }
        
        if (!"EMPLOYEE".equals(role)) {
            return "redirect:/admin";
        }

        Long employeeId = (Long) session.getAttribute("employeeId");
        Employee employee = employeeService.getEmployeeById(employeeId).orElse(null);
        
        if (employee != null) {
            model.addAttribute("selectedEmployee", employee);
            model.addAttribute("leaveRequests", leaveService.getLeaveRequestsByEmployee(employeeId));
            
            // Set up blank leave request for form binding
            LeaveRequest newRequest = new LeaveRequest();
            newRequest.setEmployee(employee);
            model.addAttribute("leaveRequest", newRequest);
            model.addAttribute("leaveTypes", LeaveType.values());
        }

        model.addAttribute("sessionName", session.getAttribute("name"));
        model.addAttribute("sessionRole", role);
        return "leave-request";
    }

    @PostMapping("/leaves/request")
    public String submitLeave(HttpSession session,
                              @RequestParam("startDate") String startDateStr,
                              @RequestParam("endDate") String endDateStr,
                              @RequestParam("leaveType") String leaveTypeStr,
                              @RequestParam("reason") String reason,
                              RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"EMPLOYEE".equals(role)) {
            return "redirect:/login";
        }

        Long employeeId = (Long) session.getAttribute("employeeId");
        try {
            Employee employee = employeeService.getEmployeeById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            LeaveType leaveType = LeaveType.valueOf(leaveTypeStr.toUpperCase());

            LeaveRequest request = new LeaveRequest(employee, startDate, endDate, 0, leaveType, reason);
            leaveService.submitLeaveRequest(request);

            redirectAttributes.addFlashAttribute("successMessage", "Leave request submitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error submitting leave request: " + e.getMessage());
        }

        return "redirect:/leaves";
    }

    @GetMapping("/payslip/{id}")
    public String payslipDetail(@PathVariable("id") Long id, HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if (role == null) {
            return "redirect:/login";
        }

        Payslip payslip = payrollService.getPayslipById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payslip not found: " + id));
        
        // Enforce that employees can only view their own payslips
        if ("EMPLOYEE".equals(role)) {
            Long employeeId = (Long) session.getAttribute("employeeId");
            if (!payslip.getEmployee().getId().equals(employeeId)) {
                return "redirect:/login"; // Unauthorized access redirect
            }
        }

        model.addAttribute("payslip", payslip);
        return "payslip-detail";
    }
}
