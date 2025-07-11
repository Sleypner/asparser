package dev.sleypner.asparser.config;

import dev.sleypner.asparser.domain.model.Role;
import dev.sleypner.asparser.domain.model.User;
import dev.sleypner.asparser.domain.model.UserActionLog;
import dev.sleypner.asparser.service.core.auth.log.UserActionLogsService;
import dev.sleypner.asparser.service.core.auth.roles.RolesService;
import dev.sleypner.asparser.service.core.auth.user.UsersService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CustomOauth2UserService extends DefaultOAuth2UserService {
    RolesService rolesService;
    UsersService usersService;
    UserActionLogsService userActionLogsService;
    Environment environment;

    public CustomOauth2UserService(RolesService rolesService, UsersService usersService, Environment environment, UserActionLogsService userActionLogsService) {
        this.rolesService = rolesService;
        this.usersService = usersService;
        this.environment = environment;
        this.userActionLogsService = userActionLogsService;
    }

    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

        return (userRequest) -> {
            OAuth2User oAuth2User = delegate.loadUser(userRequest);

            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

            Map<String, Object> attributes = oAuth2User.getAttributes();
            Collection<? extends GrantedAuthority> authorities = oAuth2User.getAuthorities();

            UserActionLog userAction = UserActionLog.getAction(null, request, "login");
            User user = saveOauth2Users(userRequest, authorities, attributes, userAction);


            return new CustomOauth2User(oAuth2User, userRequest.getClientRegistration().getClientName(), user.getAttributes());
        };
    }

    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {

            OidcUser oidcUser = delegate.loadUser(userRequest);
            Map<String, Object> attributes = oidcUser.getAttributes();
            Collection<? extends GrantedAuthority> authorities = oidcUser.getAuthorities();

            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

            UserActionLog userAction = UserActionLog.getAction(null, request, "login");
            User user = saveOauth2Users(userRequest, authorities, attributes, userAction);

            return new CustomOidcUser(oidcUser, userRequest.getClientRegistration().getClientName(), user.getAttributes());
        };
    }

    private User saveOauth2Users(OAuth2UserRequest request,
                                 Collection<? extends GrantedAuthority> authorities,
                                 Map<String, Object> attributes,
                                 UserActionLog userAction) {
        String token = request.getAccessToken().getTokenValue();
        String providerId = request.getClientRegistration().getRegistrationId();

        User user = new User();
        switch (providerId) {
            case "google" -> {
                String username = (String) attributes.get("given_name");

                user = userPreparation(username.toLowerCase(), userAction)
                        .setExternalId((String) attributes.get("sub"))
                        .setEmail((String) attributes.get("email"))
                        .setName((String) attributes.get("given_name"))
                        .setFirstName((String) attributes.get("given_name"))
                        .setLastName((String) attributes.get("family_name"))
                        .setImg((String) attributes.get("picture"));

            }
            case "discord" -> {
                String username = (String) attributes.get("username");

                user = userPreparation(username.toLowerCase(), userAction)
                        .setExternalId((String) attributes.get("id"))
                        .setEmail((String) attributes.get("email"))
                        .setName((String) attributes.get("global_name"))
                        .setImg((String) attributes.get("avatar"));

            }
            case "yandex" -> {
                String username = (String) attributes.get("login");
                Map<String, Object> phones = (Map<String, Object>) attributes.get("default_phone");
                String birthday = (String) attributes.get("birthday");

                user = userPreparation(username.toLowerCase(), userAction)
                        .setExternalId((String) attributes.get("id"))
                        .setEmail((String) attributes.get("default_email"))
                        .setName((String) attributes.get("login"))
                        .setFirstName((String) attributes.get("first_name"))
                        .setLastName((String) attributes.get("last_name"))
                        .setBirthday(!Objects.equals(birthday, "") ? LocalDateTime.parse(birthday) : null)
                        .setGender((String) attributes.get("gender"))
                        .setPhone((String) phones.get("number"))
                        .setImg((String) attributes.get("default_avatar_id"));
            }
        }
        user.setEnabled(true)
                .setOauth(1)
                .setToken(token)
                .setProvider(providerId);
        user = usersService.save(user);
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OAuth2UserAuthority) {
                List<Role> savedRoles = rolesService.getByUserId(user.getId());
                if (savedRoles.isEmpty()) {
                    rolesService.save(Role.builder()
                            .username(user.getUsername())
                            .role(authority.getAuthority())
                            .user(user).build());
                }
            }
        }

        userAction.setUser(user)
                .setActionType("oauth2 login");
        userActionLogsService.save(userAction);

        return user;
    }

    public User userPreparation(String username, UserActionLog userAction) {
        return usersService.getByUsername(username).orElseGet(() -> new User()
                .setUsername(username)
                .setUserActionLogs(Set.of(userAction)));
    }
}
