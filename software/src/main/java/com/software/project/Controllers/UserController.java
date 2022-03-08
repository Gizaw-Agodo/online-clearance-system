package com.software.project.Controllers;

import java.time.Duration;
import java.time.LocalDateTime;

import com.software.project.security.Choice;
import com.software.project.security.User;
import com.software.project.security.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;

@Controller
public class UserController {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private UserRepository userRepo;

    @ModelAttribute("choice")
    public Choice getChoice() {
        return new Choice();
    }

    @ModelAttribute("user")
    public User getUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // return userRepo.findByUsername(auth.getName());
        return userRepo.findByUsername(auth.getName());
    }

    @ModelAttribute("reason")
    public String[] getReason() {
        return new String[] {
                "WITHDRAWAL", "END OF ACADEMY CALENDER", "GRADUATION", "DROP OUT"
        };
    }

    @ModelAttribute("offices")
    public String[] getOffices() {
        return new String[] {
                "REGISTRAR", "DORMITORY", "CAFETERIA"
        };
    }

    @GetMapping("/request")
    public String requestHandler(Model model) {

        model.addAttribute("message", "instruction");

        return "main";
    }

    @PostMapping("/clearance/request")
    public String clearanceRequest(Model model, @ModelAttribute Choice choice) {

        String[] reasons = choice.getReason();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findByUsername(auth.getName());

        for (String reason : reasons) {
            user.setReason(reason);
        }

        String email = user.getEmail();

        try {
            String jsonLibrary = restTemplate.getForObject("http://localhost:8082/status/" + email,
                    String.class);
            String jsonSport = restTemplate.getForObject("http://localhost:8083/status/" + email, String.class);

            user.setLibraryStatus(jsonLibrary);
            user.setSportStatus(jsonSport);

            if (jsonLibrary == null || jsonSport == null) {
                user.setStatus(true);

                LocalDateTime now = LocalDateTime.now();
                user.setDate(now);
                user.setLibraryStatus("true");
                user.setSportStatus("true");

            }

            else if (jsonLibrary.equalsIgnoreCase("true") && jsonSport.equalsIgnoreCase("true")) {
                user.setStatus(true);

                LocalDateTime now = LocalDateTime.now();
                user.setDate(now);

            } else {
                user.setStatus(false);
            }

            userRepo.save(user);
            model.addAttribute("message", "successRequest");
            return "main";

        } catch (Exception e) {
            model.addAttribute("message", "notListening");
            return "main";
        }

    }

    @PostMapping("/clearance/upload")
    public String uploadClearance(Model model, @ModelAttribute Choice choice) {

        String[] selectedOffice = choice.getOffices();
        for (int i = 0; i < selectedOffice.length; i++) {

            System.out.println(selectedOffice[i]);
        }
        try {
            LocalDateTime now = LocalDateTime.now();

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userRepo.findByUsername(auth.getName());

            String email = user.getEmail();

            LocalDateTime storedDate = user.getDate();

            Duration duration = Duration.between(storedDate, now);

            Long period = duration.toMinutes();


            if (user.getStatus() != true || user == null) {

                model.addAttribute("message", "notAllowed");

                return "main";
            }

            else if (period >= 1) {

                model.addAttribute("message", "outOfTime");

                return "main";

            }

            else{
                for (String office : selectedOffice) {
                    System.out.println(office);
                    if (office.equalsIgnoreCase("REGISTRAR")) {
    
                        restTemplate.put("http://localhost:8084/setStatus/" + email, user);
                    }
    
                    if (office.equalsIgnoreCase("DORMITORY")) {
    
                        restTemplate.put("http://localhost:8085/setStatus/" + email, user);
                    }
    
                    if (office.equalsIgnoreCase("CAFETERIA")) {
    
                        restTemplate.put("http://localhost:8086/setStatus/" + email, user);
                    }
                }
                model.addAttribute("message", "success");
                return "main";
            }

          

        }

        catch (Exception e) {
            model.addAttribute("message", "notListening");
            return "main";
        }

    }

}
