package com.example.leavepayroll.controller;

import com.example.leavepayroll.model.LeaveRequest;
import com.example.leavepayroll.service.LeaveService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final LeaveService leaveService;

    @Autowired
    public GlobalControllerAdvice(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @ModelAttribute
    public void addGlobalAttributes(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if ("ADMIN".equals(role)) {
            long pendingLeavesCount = leaveService.getAllLeaveRequests().stream()
                    .filter(lr -> lr.getStatus() == LeaveRequest.LeaveStatus.PENDING)
                    .count();
            model.addAttribute("pendingLeavesCount", pendingLeavesCount);
        }
    }
}
