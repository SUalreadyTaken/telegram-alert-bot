package com.su.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "idle")
public class Idle {

  @Id
  private String id;

  boolean alternative;

  public Idle() {
  }

  public Idle(boolean alternative) {
    this.alternative = alternative;
  }

  public boolean isAlternative() {
    return alternative;
  }

  public void setAlternative(boolean alternative) {
    this.alternative = alternative;
  }
}
