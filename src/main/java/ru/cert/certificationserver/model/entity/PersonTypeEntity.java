package ru.cert.certificationserver.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "person_types")
public class PersonTypeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false)
  private String name;

  public PersonTypeEntity() {
  }

  public PersonTypeEntity(Integer id, String name) {
    this.id = id;
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
