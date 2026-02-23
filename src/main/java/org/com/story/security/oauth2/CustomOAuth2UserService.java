package org.com.story.security.oauth2;

import lombok.RequiredArgsConstructor;
import org.com.story.common.AuthProvider;
import org.com.story.entity.User;
import org.com.story.repository.RoleRepository;
import org.com.story.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String googleId = (String) attributes.get("sub");
        String name = (String) attributes.get("name");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {

                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName(name);
                    newUser.setProvider(AuthProvider.GOOGLE);
                    newUser.setProviderId(googleId);
                    newUser.setEnabled(true);

                    newUser.setRoles(
                            Set.of(
                                    roleRepository
                                            .findByName("READER")
                                            .orElseThrow(() -> new IllegalStateException("Default role READER not found"))
                            )
                    );

                    return userRepository.save(newUser);
                });

        return new CustomOAuth2User(user, attributes);
    }
}
