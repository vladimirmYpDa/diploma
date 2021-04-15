package com.diploma.app;

//import com.diploma.app.service.TransportationServiceImpl;
//import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@SpringBootApplication
public class AppApplication {
    private static Resource location = new ClassPathResource("files/");

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

//    @Bean
//    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
//        return args -> {
//            TransportationServiceImpl service = (TransportationServiceImpl) ctx.getBean(TransportationServiceImpl.class);
//            service.process(location.createRelative("InitDataTestSmall.xlsx").getInputStream());
//        };
//    }
}
