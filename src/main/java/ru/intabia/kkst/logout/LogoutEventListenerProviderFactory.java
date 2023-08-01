package ru.intabia.kkst.logout;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class LogoutEventListenerProviderFactory implements EventListenerProviderFactory {
  private static final String LISTENER_ID = "event-listener-extension";

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new LogoutEventListenerProvider(session);
  }

  @Override
  public void init(Config.Scope config) {
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
  }

  @Override
  public void close() {

  }

  @Override
  public String getId() {
    return LISTENER_ID;
  }
}
