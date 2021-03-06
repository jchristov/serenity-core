package net.serenitybdd.core.buildinfo;

import net.serenitybdd.core.collect.NewList;
import java.util.HashMap;
import com.google.inject.Inject;
import net.thucydides.core.webdriver.Configuration;
import org.openqa.selenium.Capabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.nio.file.Files.newInputStream;

/**
 * Created by john on 12/02/15.
 */
public class PropertyBasedDriverCapabilityRecord implements DriverCapabilityRecord {

    Logger LOGGER = LoggerFactory.getLogger(PropertyBasedDriverCapabilityRecord.class);

    private Configuration configuration;

    @Inject
    public PropertyBasedDriverCapabilityRecord(Configuration configuration) {
        this.configuration = configuration;
    }

    public void registerCapabilities(String driver, Capabilities capabilities) {

        Properties properties = new Properties();
        properties.setProperty("platform", capabilities.getPlatform().name());
        for (String capability : capabilities.asMap().keySet()) {
            if (capabilities.getCapability(capability) instanceof String) {
                properties.setProperty(capability, capabilities.getCapability(capability).toString());
            }
        }
        try {
            File browserProperties = new File(configuration.getOutputDirectory(), "browser-" + driver.toLowerCase() + ".properties");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(browserProperties), StandardCharsets.UTF_8.toString()))  {
                properties.store(writer, "");
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to store browser configuration for " + capabilities);
        }
    }

    @Override
    public List<String> getDrivers() {
        List<String> drivers = new ArrayList<>();
        try (DirectoryStream<Path> stream = driverCapabilityRecords()) {
            for (Path file : stream) {
                drivers.add(driverNameFrom(file));
            }
        } catch (IOException | DirectoryIteratorException x) {
            LOGGER.error("Exception during getting drivers", x);
        }
        return drivers;
    }

    private String driverNameFrom(Path file) {
        return file.getFileName().toString().replace("browser-","").replace(".properties","");
    }

    private DirectoryStream<Path> driverCapabilityRecords() throws IOException {
        Path outputDirectory = configuration.getOutputDirectory().toPath();
        return Files.newDirectoryStream(outputDirectory,"browser-*.properties");
    }

    @Override
    public Map<String, Properties> getDriverCapabilities() {
        Map<String, Properties> driverCapabilities = new HashMap();
        try (DirectoryStream<Path> stream = driverCapabilityRecords()) {
            for (Path file : stream) {
                String driverName = driverNameFrom(file);
                Properties driverProperties = new Properties();
                try(InputStream properties = newInputStream(file)) {
                    driverProperties.load(properties);
                }
                driverCapabilities.put(driverName, driverProperties);
            }
        } catch (IOException | DirectoryIteratorException x) {
            LOGGER.error("Exception during getting driver capabilities",x);
        }
        return driverCapabilities;
    }

}
