package com.amalitech.user.service;

import com.amalitech.user.service.dto.request.UpdateUserProfileRequest;
import com.amalitech.user.service.dto.response.UserProfileResponse;
import com.amalitech.user.service.exception.ProfileNotFoundException;
import com.amalitech.user.service.mapper.UserProfileMapper;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.UserProfile;
import com.amalitech.user.service.model.enums.GuidedTourStatus;
import com.amalitech.user.service.model.enums.PremiumTier;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.UserProfileRepository;
import com.amalitech.user.service.service.impl.UserProfileServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService Implementation Tests")
class UserProfileServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private UUID testUserId;
    private UserProfile testProfile;
    private UserProfileResponse testResponse;
    private UpdateUserProfileRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        User testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("patrick@example.com");
        testUser.setState(UserState.ACTIVE);
        testUser.setIsVerified(true);
        testUser.setTourStatus(GuidedTourStatus.COMPLETED);
        testUser.setRole(Role.USER);
        testUser.setPremiumTier(PremiumTier.FREE);

        testProfile = new UserProfile();
        testProfile.setUserId(testUserId);
        testProfile.setUser(testUser);
        testProfile.setFullName("Patrick Appiah");
        testProfile.setAvatarUrl("https://example.com/avatar.jpg");
        testProfile.setBio("Software developer");
        testProfile.setEmailNotifications(true);
        testProfile.setPushNotifications(true);
        testProfile.setCreatedAt(LocalDateTime.now().minusDays(10));
        testProfile.setUpdatedAt(LocalDateTime.now());

        testResponse = new UserProfileResponse(
                testUserId,
                "patrick@example.com",
                "Patrick Appiah",
                "https://example.com/avatar.jpg",
                "Software developer",
                UserState.ACTIVE,
                true,
                GuidedTourStatus.COMPLETED,
                Role.USER,
                PremiumTier.FREE,
                true,
                true,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now()
        );

        updateRequest = new UpdateUserProfileRequest(
                "Kwadwo Appiah",
                "https://example.com/new-avatar.jpg",
                "Senior Software Engineer",
                false,
                true
        );
    }

    @Nested
    @DisplayName("getUserProfile Tests")
    class GetUserProfileTests {

        @Test
        @DisplayName("Should successfully retrieve user profile when profile exists")
        void shouldGetUserProfileSuccessfully() {
            // Arrange
            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            UserProfileResponse result = userProfileService.getUserProfile(testUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testResponse);

            verify(userProfileRepository).findByUserId(testUserId);
            verify(userProfileMapper).toResponse(testProfile);
            verifyNoMoreInteractions(userProfileRepository, userProfileMapper);
        }

        @Test
        @DisplayName("Should throw ProfileNotFoundException when profile does not exist")
        void shouldThrowExceptionWhenProfileNotFound() {
            // Arrange
            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userProfileService.getUserProfile(testUserId))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessage("Profile not found for user: " + testUserId);

            verify(userProfileRepository).findByUserId(testUserId);
            verify(userProfileMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should handle profile with minimal data")
        void shouldHandleProfileWithMinimalData() {
            // Arrange
            User minimalUser = new User();
            minimalUser.setId(testUserId);
            minimalUser.setEmail("patrick@example.com");
            minimalUser.setState(UserState.REGISTERED);
            minimalUser.setIsVerified(false);
            minimalUser.setTourStatus(GuidedTourStatus.NOT_STARTED);
            minimalUser.setRole(Role.USER);
            minimalUser.setPremiumTier(PremiumTier.FREE);

            UserProfile minimalProfile = new UserProfile();
            minimalProfile.setUserId(testUserId);
            minimalProfile.setUser(minimalUser);
            minimalProfile.setEmailNotifications(true);
            minimalProfile.setPushNotifications(true);

            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(minimalProfile));
            when(userProfileMapper.toResponse(minimalProfile))
                    .thenReturn(testResponse);

            // Act
            UserProfileResponse result = userProfileService.getUserProfile(testUserId);

            // Assert
            assertThat(result).isNotNull();
            verify(userProfileRepository).findByUserId(testUserId);
            verify(userProfileMapper).toResponse(minimalProfile);
        }

        @Test
        @DisplayName("Should handle profile with all fields populated")
        void shouldHandleProfileWithAllFields() {
            // Arrange
            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            UserProfileResponse result = userProfileService.getUserProfile(testUserId);

            // Assert
            assertThat(result).isNotNull();
            verify(userProfileRepository).findByUserId(testUserId);
            verify(userProfileMapper).toResponse(testProfile);
        }
    }

    @Nested
    @DisplayName("updateUserProfile Tests")
    class UpdateUserProfileTests {

        @Test
        @DisplayName("Should successfully update all profile fields")
        void shouldUpdateAllProfileFieldsSuccessfully() {
            // Arrange
            User updatedUser = new User();
            updatedUser.setId(testUserId);
            updatedUser.setEmail("patrick@example.com");
            updatedUser.setState(UserState.ACTIVE);
            updatedUser.setIsVerified(true);
            updatedUser.setTourStatus(GuidedTourStatus.COMPLETED);
            updatedUser.setRole(Role.USER);
            updatedUser.setPremiumTier(PremiumTier.FREE);

            UserProfile updatedProfile = new UserProfile();
            updatedProfile.setUserId(testUserId);
            updatedProfile.setUser(updatedUser);
            updatedProfile.setFullName("Kwadwo Appiah");
            updatedProfile.setAvatarUrl("https://example.com/new-avatar.jpg");
            updatedProfile.setBio("Senior Software Engineer");
            updatedProfile.setEmailNotifications(false);
            updatedProfile.setPushNotifications(true);

            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            doNothing().when(userProfileMapper).updateEntityFromRequest(testProfile, updateRequest);
            when(userProfileRepository.save(testProfile))
                    .thenReturn(updatedProfile);
            when(userProfileMapper.toResponse(updatedProfile))
                    .thenReturn(testResponse);

            // Act
            UserProfileResponse result = userProfileService.updateUserProfile(testUserId, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testResponse);

            verify(userProfileRepository).findByUserId(testUserId);
            verify(userProfileMapper).updateEntityFromRequest(testProfile, updateRequest);
            verify(userProfileRepository).save(testProfile);
            verify(userProfileMapper).toResponse(updatedProfile);
        }

        @Test
        @DisplayName("Should throw ProfileNotFoundException when profile does not exist for update")
        void shouldThrowExceptionWhenProfileNotFoundForUpdate() {
            // Arrange
            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userProfileService.updateUserProfile(testUserId, updateRequest))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessage("Profile not found for user: " + testUserId);

            verify(userProfileRepository).findByUserId(testUserId);
            verify(userProfileMapper, never()).updateEntityFromRequest(any(), any());
            verify(userProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update only fullName when other fields are null")
        void shouldUpdateOnlyFullName() {
            // Arrange
            UpdateUserProfileRequest partialRequest = new UpdateUserProfileRequest(
                    "Updated Name",
                    null,
                    null,
                    null,
                    null
            );

            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            UserProfileResponse result = userProfileService.updateUserProfile(testUserId, partialRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(userProfileMapper).updateEntityFromRequest(testProfile, partialRequest);
            verify(userProfileRepository).save(testProfile);
        }

        @Test
        @DisplayName("Should update notification preferences only")
        void shouldUpdateNotificationPreferencesOnly() {
            // Arrange
            UpdateUserProfileRequest notificationRequest = new UpdateUserProfileRequest(
                    null,
                    null,
                    null,
                    false,
                    false
            );

            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            UserProfileResponse result = userProfileService.updateUserProfile(testUserId, notificationRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(userProfileMapper).updateEntityFromRequest(testProfile, notificationRequest);
            verify(userProfileRepository).save(testProfile);
        }

        @Test
        @DisplayName("Should handle empty update request with all null fields")
        void shouldHandleEmptyUpdateRequest() {
            // Arrange
            UpdateUserProfileRequest emptyRequest = new UpdateUserProfileRequest(
                    null, null, null, null, null
            );

            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            UserProfileResponse result = userProfileService.updateUserProfile(testUserId, emptyRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(userProfileMapper).updateEntityFromRequest(testProfile, emptyRequest);
            verify(userProfileRepository).save(testProfile);
        }

        @Test
        @DisplayName("Should update avatar URL and bio")
        void shouldUpdateAvatarAndBio() {
            // Arrange
            UpdateUserProfileRequest avatarBioRequest = new UpdateUserProfileRequest(
                    null,
                    "https://example.com/professional-avatar.jpg",
                    "Experienced developer with 10+ years in the industry",
                    null,
                    null
            );

            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            UserProfileResponse result = userProfileService.updateUserProfile(testUserId, avatarBioRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(userProfileMapper).updateEntityFromRequest(testProfile, avatarBioRequest);
            verify(userProfileRepository).save(testProfile);
        }

        @Test
        @DisplayName("Should verify mapper is called before save")
        void shouldCallMapperBeforeSave() {
            // Arrange
            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            userProfileService.updateUserProfile(testUserId, updateRequest);

            // Assert - verify order of operations
            var inOrder = inOrder(userProfileMapper, userProfileRepository);
            inOrder.verify(userProfileMapper).updateEntityFromRequest(testProfile, updateRequest);
            inOrder.verify(userProfileRepository).save(testProfile);
            inOrder.verify(userProfileMapper).toResponse(testProfile);
        }
    }

    @Nested
    @DisplayName("deleteUserProfile Tests")
    class DeleteUserProfileTests {

        @Test
        @DisplayName("Should successfully delete user profile when profile exists")
        void shouldDeleteUserProfileSuccessfully() {
            // Arrange
            when(userProfileRepository.existsByUserId(testUserId))
                    .thenReturn(true);
            doNothing().when(userProfileRepository).deleteByUserId(testUserId);

            // Act
            userProfileService.deleteUserProfile(testUserId);

            // Assert
            verify(userProfileRepository).existsByUserId(testUserId);
            verify(userProfileRepository).deleteByUserId(testUserId);
            verifyNoMoreInteractions(userProfileRepository);
        }

        @Test
        @DisplayName("Should throw ProfileNotFoundException when profile does not exist for deletion")
        void shouldThrowExceptionWhenProfileNotFoundForDeletion() {
            // Arrange
            when(userProfileRepository.existsByUserId(testUserId))
                    .thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> userProfileService.deleteUserProfile(testUserId))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessage("Profile not found for user: " + testUserId);

            verify(userProfileRepository).existsByUserId(testUserId);
            verify(userProfileRepository, never()).deleteByUserId(any());
        }

        @Test
        @DisplayName("Should verify existence check happens before deletion")
        void shouldVerifyExistenceBeforeDeletion() {
            // Arrange
            when(userProfileRepository.existsByUserId(testUserId))
                    .thenReturn(true);
            doNothing().when(userProfileRepository).deleteByUserId(testUserId);

            // Act
            userProfileService.deleteUserProfile(testUserId);

            // Assert - verify order of operations
            var inOrder = inOrder(userProfileRepository);
            inOrder.verify(userProfileRepository).existsByUserId(testUserId);
            inOrder.verify(userProfileRepository).deleteByUserId(testUserId);
        }

        @Test
        @DisplayName("Should not call delete when profile doesn't exist")
        void shouldNotCallDeleteWhenProfileDoesNotExist() {
            // Arrange
            when(userProfileRepository.existsByUserId(testUserId))
                    .thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> userProfileService.deleteUserProfile(testUserId))
                    .isInstanceOf(ProfileNotFoundException.class);

            verify(userProfileRepository).existsByUserId(testUserId);
            verify(userProfileRepository, never()).deleteByUserId(testUserId);
        }

        @Test
        @DisplayName("Should handle multiple deletion attempts for same user")
        void shouldHandleMultipleDeletionAttempts() {
            // Arrange - first call succeeds
            when(userProfileRepository.existsByUserId(testUserId))
                    .thenReturn(true)
                    .thenReturn(false); // second call should fail

            // Act - first deletion
            userProfileService.deleteUserProfile(testUserId);

            // Assert - second deletion should throw exception
            assertThatThrownBy(() -> userProfileService.deleteUserProfile(testUserId))
                    .isInstanceOf(ProfileNotFoundException.class);

            verify(userProfileRepository, times(2)).existsByUserId(testUserId);
            verify(userProfileRepository, times(1)).deleteByUserId(testUserId);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long bio within limits")
        void shouldHandleVeryLongBio() {
            // Arrange
            String longBio = "A".repeat(1000); // Max length per validation
            UpdateUserProfileRequest longBioRequest = new UpdateUserProfileRequest(
                    null,
                    null,
                    longBio,
                    null,
                    null
            );

            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            UserProfileResponse result = userProfileService.updateUserProfile(testUserId, longBioRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(userProfileMapper).updateEntityFromRequest(testProfile, longBioRequest);
        }

        @Test
        @DisplayName("Should handle very long full name within limits")
        void shouldHandleVeryLongFullName() {
            // Arrange
            String longName = "A".repeat(255); // Max length per validation
            UpdateUserProfileRequest longNameRequest = new UpdateUserProfileRequest(
                    longName,
                    null,
                    null,
                    null,
                    null
            );

            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            UserProfileResponse result = userProfileService.updateUserProfile(testUserId, longNameRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(userProfileMapper).updateEntityFromRequest(testProfile, longNameRequest);
        }

        @Test
        @DisplayName("Should handle different UUID formats")
        void shouldHandleDifferentUuidFormats() {
            // Arrange
            UUID randomUuid = UUID.randomUUID();
            when(userProfileRepository.findByUserId(randomUuid))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            UserProfileResponse result = userProfileService.getUserProfile(randomUuid);

            // Assert
            assertThat(result).isNotNull();
            verify(userProfileRepository).findByUserId(randomUuid);
        }

        @Test
        @DisplayName("Should handle toggling notifications from true to false")
        void shouldToggleNotificationsToFalse() {
            // Arrange
            UpdateUserProfileRequest disableNotifications = new UpdateUserProfileRequest(
                    null,
                    null,
                    null,
                    false,
                    false
            );

            testProfile.setEmailNotifications(true);
            testProfile.setPushNotifications(true);

            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            userProfileService.updateUserProfile(testUserId, disableNotifications);

            // Assert
            verify(userProfileMapper).updateEntityFromRequest(testProfile, disableNotifications);
        }

        @Test
        @DisplayName("Should handle toggling notifications from false to true")
        void shouldToggleNotificationsToTrue() {
            // Arrange
            UpdateUserProfileRequest enableNotifications = new UpdateUserProfileRequest(
                    null,
                    null,
                    null,
                    true,
                    true
            );

            testProfile.setEmailNotifications(false);
            testProfile.setPushNotifications(false);

            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act
            userProfileService.updateUserProfile(testUserId, enableNotifications);

            // Assert
            verify(userProfileMapper).updateEntityFromRequest(testProfile, enableNotifications);
        }
    }

    @Nested
    @DisplayName("Transaction Verification Tests")
    class TransactionTests {

        @Test
        @DisplayName("Should execute getUserProfile without throwing exceptions")
        void verifyGetUserProfileTransactional() {
            // Arrange
            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act & Assert
            assertThatCode(() -> userProfileService.getUserProfile(testUserId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should execute updateUserProfile without throwing exceptions")
        void verifyUpdateUserProfileTransactional() {
            // Arrange
            when(userProfileRepository.findByUserId(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(userProfileRepository.save(testProfile))
                    .thenReturn(testProfile);
            when(userProfileMapper.toResponse(testProfile))
                    .thenReturn(testResponse);

            // Act & Assert
            assertThatCode(() -> userProfileService.updateUserProfile(testUserId, updateRequest))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should execute deleteUserProfile without throwing exceptions")
        void verifyDeleteUserProfileTransactional() {
            // Arrange
            when(userProfileRepository.existsByUserId(testUserId))
                    .thenReturn(true);

            // Act & Assert
            assertThatCode(() -> userProfileService.deleteUserProfile(testUserId))
                    .doesNotThrowAnyException();
        }
    }
}