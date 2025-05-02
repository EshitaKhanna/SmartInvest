package com.example.investmentplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.investmentplatform.dto.AuthenticationRequest;
import com.example.investmentplatform.dto.AuthenticationResponse;
import com.example.investmentplatform.entity.User;
import com.example.investmentplatform.enums.InvestmentGoal;
import com.example.investmentplatform.enums.RiskTolerance;
import com.example.investmentplatform.repository.UserRepository;
import com.example.investmentplatform.util.JwtUtil;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthenticationRequest authenticationRequest) {
        System.out.println("Registering user: " + authenticationRequest.getEmail());
    
        if (userRepository.findByEmail(authenticationRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists!");
        }
    
        User newUser = new User();
        newUser.setEmail(authenticationRequest.getEmail());
        newUser.setPassword(passwordEncoder.encode(authenticationRequest.getPassword())); // Encrypt password
        newUser.setFirstName(authenticationRequest.getFirstName());
        newUser.setLastName(authenticationRequest.getLastName());
        newUser.setDob(authenticationRequest.getDob());
        newUser.setAnnualIncome(authenticationRequest.getAnnualIncome());
        newUser.setInvestmentAmount(authenticationRequest.getInvestmentAmount());
        try {
            newUser.setMainInvestmentGoal(InvestmentGoal.valueOf(authenticationRequest.getMainInvestmentGoal().toUpperCase()));
            newUser.setRiskTolerance(RiskTolerance.valueOf(authenticationRequest.getRiskTolerance().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid investment goal or risk tolerance value.");
        }

        System.out.print(newUser.getInvestmentAmount());

        userRepository.save(newUser);
        return ResponseEntity.ok("User registered successfully");
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        try {
            System.out.println("Login attempt for: " + authenticationRequest.getEmail());  // Debugging line
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getEmail(),
                            authenticationRequest.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new Exception("Incorrect email or password", e);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getEmail());
        final String token = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthenticationResponse(token));
    }
}