package com.example.leavepayroll.controller;

import com.example.leavepayroll.model.Employee;
import com.example.leavepayroll.repository.EmployeeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class LoginController {

    private final EmployeeRepository employeeRepository;

    @Autowired
    public LoginController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        // Redirect if already logged in
        String role = (String) session.getAttribute("role");
        if ("ADMIN".equals(role)) {
            return "redirect:/admin";
        } else if ("EMPLOYEE".equals(role)) {
            Long employeeId = (Long) session.getAttribute("employeeId");
            return "redirect:/?employeeId=" + employeeId;
        }

        // Add employees list to let the user select test accounts easily
        model.addAttribute("allEmployees", employeeRepository.findAll());
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("email") String email,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        email = email.trim();
        
        if ("admin@portal.com".equalsIgnoreCase(email)) {
            session.setAttribute("role", "ADMIN");
            session.setAttribute("email", "admin@portal.com");
            session.setAttribute("name", "HR Administrator");
            return "redirect:/admin";
        }

        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            session.setAttribute("role", "EMPLOYEE");
            session.setAttribute("employeeId", employee.getId());
            session.setAttribute("email", employee.getEmail());
            session.setAttribute("name", employee.getName());
            return "redirect:/?employeeId=" + employee.getId();
        }

        redirectAttributes.addFlashAttribute("errorMessage", "Invalid email address. Access denied.");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
