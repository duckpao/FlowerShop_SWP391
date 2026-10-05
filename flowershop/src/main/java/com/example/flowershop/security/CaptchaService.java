package com.example.flowershop.security;

import com.example.flowershop.exception.CaptchaRequiredException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/** Per-instance challenges and login counters. OTP failure counts remain in the database. */
@Service
public class CaptchaService {
    private final SecureRandom random = new SecureRandom();
    private record Challenge(String key, String answer, Instant expires) {}
    private record Attempts(int count, Instant expires) {}
    private final Map<String, Challenge> challenges = new HashMap<>();
    private final Map<String, Attempts> failures = new HashMap<>();
    public record Image(String id, String image, int expiresIn) {}
    private String key(String email, String purpose) { return purpose + ":" + email.strip().toLowerCase(Locale.ROOT); }
    private void clean() {
        challenges.values().removeIf(c -> !Instant.now().isBefore(c.expires()));
        failures.values().removeIf(c -> !Instant.now().isBefore(c.expires()));
    }
    public synchronized boolean required(String email) { clean(); var a=failures.get(key(email,"login")); return a!=null && a.count()>=5; }
    public synchronized void failed(String email) {
        clean(); String k=key(email,"login"); var a=failures.get(k);
        if(a==null && failures.size()>=10000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        int count=a==null?1:Math.min(5,a.count()+1);
        failures.put(k,new Attempts(count,Instant.now().plusSeconds(900)));
        if(count>=5) throw new CaptchaRequiredException();
    }
    public synchronized void clear(String email) { failures.remove(key(email,"login")); }
    public synchronized Image create(String email,String purpose) {
        if(!Set.of("login","register","reset").contains(purpose)) throw new IllegalArgumentException("Loại CAPTCHA không hợp lệ.");
        clean(); if(challenges.size()>=1000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        String alphabet="23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; StringBuilder answer=new StringBuilder();
        var image=new BufferedImage(210,64,BufferedImage.TYPE_INT_RGB); var g=image.createGraphics();
        g.setColor(new Color(240,246,241)); g.fillRect(0,0,210,64); g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,32));
        for(int i=0;i<5;i++){char ch=alphabet.charAt(random.nextInt(alphabet.length()));answer.append(ch);g.setColor(new Color(30+random.nextInt(60),60+random.nextInt(60),40+random.nextInt(60)));g.drawString(String.valueOf(ch),15+i*38,40+random.nextInt(10));}
        g.setColor(new Color(125,155,135));for(int i=0;i<7;i++)g.drawLine(random.nextInt(210),random.nextInt(64),random.nextInt(210),random.nextInt(64));g.dispose();
        try {var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);String id=UUID.randomUUID().toString();challenges.put(id,new Challenge(key(email,purpose),answer.toString(),Instant.now().plusSeconds(120)));return new Image(id,"data:image/png;base64,"+Base64.getEncoder().encodeToString(bytes.toByteArray()),120);}
        catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    public synchronized void verify(String email,String purpose,String id,String answer) {
        var c=id==null?null:challenges.remove(id);
        if(c==null || !c.key().equals(key(email,purpose)) || !Instant.now().isBefore(c.expires()) || answer==null || !c.answer().equalsIgnoreCase(answer.strip())) throw new CaptchaRequiredException();
    }
}
