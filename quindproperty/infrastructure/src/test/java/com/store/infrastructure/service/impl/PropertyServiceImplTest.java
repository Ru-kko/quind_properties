package com.store.infrastructure.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import com.store.domain.table.Property;
import com.store.dto.PropertyRegistry;
import com.store.error.NullDataError;
import com.store.error.PropertyError;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Sql(scripts = { "/dataIngest.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = { "/dataDrop.sql" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class PropertyServiceImplTest {
  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private PropertyServiceImpl propertyService;

  // ___________________ PropertyService.Find() ___________________
  @Test
  void findWithoutBound() {
    var page = 0;
    var res = propertyService.find(null, null, page);

    var fromDb = jdbcTemplate.queryForObject("select count(*) from Property WHERE active = true AND available = true",
        Long.class);
    assertEquals(res.getTotalElements(), fromDb);
  }

  @Test
  void findWithLeftBound() {
    var page = 0;
    var res = propertyService.find(new BigDecimal(700000), null, page);

    var fromDb = jdbcTemplate.queryForObject(
        "select count(*) from Property WHERE active = true AND available = true AND price >= 700000",
        Long.class);
    assertEquals(res.getTotalElements(), fromDb);
  }

  @Test
  void findWithBothBounds() {
    var page = 0;
    var res = propertyService.find(new BigDecimal(500000), new BigDecimal(800000), page);

    var fromDb = jdbcTemplate.queryForObject("select count(*) from Property WHERE active = true AND available = true " +
        "AND (price >= 500000 AND price <= 800000)",
        Long.class);
    assertEquals(res.getTotalElements(), fromDb);
  }

  // _____________________ transform _______________________________
  void transformNullNameThrowsNullDataError() {
    PropertyRegistry dto = new PropertyRegistry();
    dto.setImage("image.jpg");
    dto.setLocation(UUID.randomUUID());
    dto.setPrice(new BigDecimal("1000000"));

    NullDataError exception = assertThrows(NullDataError.class, () -> {
      propertyService.transform(dto);
    });
    assertEquals("Not provided name", exception.getMessage());
  }

  @Test
  void transformNullImageThrowsNullDataError() {
    PropertyRegistry dto = new PropertyRegistry();
    dto.setName("Test Property");
    dto.setLocation(UUID.randomUUID());
    dto.setPrice(new BigDecimal("1000000"));

    NullDataError exception = assertThrows(NullDataError.class, () -> {
      propertyService.transform(dto);
    });
    assertEquals("Not provided image", exception.getMessage());
  }

  @Test
  void transformNullLocationThrowsNullDataError() {
    PropertyRegistry dto = new PropertyRegistry();
    dto.setName("Test Property");
    dto.setImage("image.jpg");
    dto.setPrice(new BigDecimal("1000000"));

    NullDataError exception = assertThrows(NullDataError.class, () -> {
      propertyService.transform(dto);
    });
    assertEquals("Not provided valid location id", exception.getMessage());
  }

  @Test
  void transformNonExistentCityThrowsPropertyError() {
    PropertyRegistry dto = new PropertyRegistry();
    dto.setName("Test Property");
    dto.setImage("image.jpg");
    dto.setLocation(UUID.randomUUID());
    dto.setPrice(new BigDecimal("1000000"));

    PropertyError exception = assertThrows(PropertyError.class, () -> {
      propertyService.transform(dto);
    });
    assertEquals("Not found city with id " + dto.getLocation().toString(), exception.getMessage());
  }

  @Test
  void transformLowPriceInBogotaThrowsPropertyError() {
    PropertyRegistry dto = new PropertyRegistry();
    dto.setName("Test Property");
    dto.setImage("image.jpg");
    dto.setLocation(UUID.fromString("a4b2c9d7-258e-4f2f-a1ad-1c7f5f2a9d75")); // Bogota
    dto.setPrice(new BigDecimal("1000000"));

    PropertyError exception = assertThrows(PropertyError.class, () -> {
      propertyService.transform(dto);
    });
    assertEquals("Price must be > 2'000.000 in this city", exception.getMessage());
  }

  @Test
  void testTransformValidInputReturnsProperty() throws PropertyError {
    PropertyRegistry dto = new PropertyRegistry();
    dto.setName("Test Property");
    dto.setImage("image.jpg");
    dto.setLocation(UUID.fromString("a4b2c9d7-258e-4f2f-a1ad-1c7f5f2a9d75")); // Bogota
    dto.setPrice(new BigDecimal("2000001"));

    Property result = propertyService.transform(dto);
    assertNotNull(result);
    assertEquals(dto.getName(), result.getName());
    assertEquals(dto.getImage(), result.getImg());
    assertEquals(dto.getPrice(), result.getPrice());
    assertEquals("Bogota", result.getLocation().getName());
  }

  // ____________________ Save ___________________________________
  @Test
  void testSaveRepeatedName() {
    PropertyRegistry repeated = new PropertyRegistry("Luxury Apartment Medellin",
        UUID.fromString("e9c1a570-dbe4-4a2e-a71e-2fd5a7b7f123"), "image.png", new BigDecimal("100000"));

    assertThrows(PropertyError.class, () -> propertyService.save(repeated));
  }

  @Test
  void testSaveValidProperty() {
    PropertyRegistry newProperty = new PropertyRegistry("New Appartment In Bogota", 
        UUID.fromString("a4b2c9d7-258e-4f2f-a1ad-1c7f5f2a9d75"), "image.png", new BigDecimal("2500000.00"));

    Property savedProperty = assertDoesNotThrow(() -> propertyService.save(newProperty));

    UUID newPropertyId = savedProperty.getPropertyId();
    
    assertNotNull(newPropertyId);

    var fromDb = jdbcTemplate.queryForMap("select * from Property WHERE propertyId = ?", newPropertyId);

    var originalDate = Calendar.getInstance();
    originalDate.setTime(savedProperty.getDateCreated());

    originalDate.set(Calendar.HOUR_OF_DAY, 0);
    originalDate.set(Calendar.MINUTE, 0);
    originalDate.set(Calendar.SECOND, 0);
    originalDate.set(Calendar.MILLISECOND, 0);

    assertEquals(fromDb.get("dateCreated"), originalDate.getTime());
    assertEquals(fromDb.get("active"), savedProperty.getActive());
    assertEquals(fromDb.get("cityId"), savedProperty.getLocation().getCityId());
    assertEquals(fromDb.get("img"), savedProperty.getImg());
    assertEquals(fromDb.get("name"), savedProperty.getName());
    assertEquals(fromDb.get("price"), savedProperty.getPrice()); 
  }
}
