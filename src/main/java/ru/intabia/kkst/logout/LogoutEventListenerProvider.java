package ru.intabia.kkst.logout;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class LogoutEventListenerProvider implements EventListenerProvider {
  private final KeycloakSession session;

  public LogoutEventListenerProvider(KeycloakSession session) {
    this.session = session;
  }

  @Override
  public void onEvent(Event event) {
    if (EventType.LOGOUT.equals(event.getType()) && event.getRealmId() != null && event.getUserId() != null) {
      removeRolesOnLogout(event);
    }
  }

  private void removeRolesOnLogout(Event event) {
    RealmModel realm = session.realms().getRealm(event.getRealmId());
    org.keycloak.models.UserModel user = session.userStorageManager().getUserById(realm, event.getUserId());
    org.keycloak.models.ClientModel client = realm.getClientByClientId("account-console");
    java.util.stream.Stream<org.keycloak.models.RoleModel> roles = user.getClientRoleMappingsStream(client);

    roles.forEach(user::deleteRoleMapping);
    session.userCache().evict(realm, user);
  }

  @Override
  public void onEvent(AdminEvent event, boolean includeRepresentation) {

  }

  @Override
  public void close() {
  }
}
