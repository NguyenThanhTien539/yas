package com.yas.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.customer.model.UserAddress;
import com.yas.customer.repository.UserAddressRepository;
import com.yas.customer.viewmodel.address.ActiveAddressVm;
import com.yas.customer.viewmodel.address.AddressDetailVm;
import com.yas.customer.viewmodel.address.AddressPostVm;
import com.yas.customer.viewmodel.address.AddressVm;
import com.yas.customer.viewmodel.useraddress.UserAddressVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class UserAddressServiceTest {

    private UserAddressRepository userAddressRepository;
    private LocationService locationService;
    private UserAddressService userAddressService;

    @BeforeEach
    void setUp() {
        userAddressRepository = mock(UserAddressRepository.class);
        locationService = mock(LocationService.class);
        userAddressService = new UserAddressService(userAddressRepository, locationService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserAddressList_whenAnonymous_thenThrowAccessDeniedException() {
        setAuthenticationName("anonymousUser");

        assertThrows(AccessDeniedException.class, () -> userAddressService.getUserAddressList());
    }

    @Test
    void getUserAddressList_whenNormalCase_thenReturnSortedActiveFirst() {
        setAuthenticationName("user1");

        UserAddress active = UserAddress.builder().id(1L).userId("user1").addressId(100L).isActive(true).build();
        UserAddress inactive = UserAddress.builder().id(2L).userId("user1").addressId(200L).isActive(false).build();
        when(userAddressRepository.findAllByUserId("user1")).thenReturn(List.of(inactive, active));

        AddressDetailVm address100 = AddressDetailVm.builder()
            .id(100L)
            .contactName("A")
            .phone("1")
            .addressLine1("Line 1")
            .city("City")
            .zipCode("111")
            .districtId(1L)
            .districtName("D1")
            .stateOrProvinceId(10L)
            .stateOrProvinceName("S1")
            .countryId(100L)
            .countryName("C1")
            .build();
        AddressDetailVm address200 = AddressDetailVm.builder()
            .id(200L)
            .contactName("B")
            .phone("2")
            .addressLine1("Line 2")
            .city("City")
            .zipCode("222")
            .districtId(2L)
            .districtName("D2")
            .stateOrProvinceId(20L)
            .stateOrProvinceName("S2")
            .countryId(200L)
            .countryName("C2")
            .build();

        when(locationService.getAddressesByIdList(List.of(200L, 100L))).thenReturn(List.of(address100, address200));

        List<ActiveAddressVm> result = userAddressService.getUserAddressList();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().isActive()).isTrue();
        assertEquals(100L, result.getFirst().id());
    }

    @Test
    void getAddressDefault_whenAnonymous_thenThrowAccessDeniedException() {
        setAuthenticationName("anonymousUser");

        assertThrows(AccessDeniedException.class, () -> userAddressService.getAddressDefault());
    }

    @Test
    void getAddressDefault_whenNoActiveAddress_thenThrowNotFoundException() {
        setAuthenticationName("user1");
        when(userAddressRepository.findByUserIdAndIsActiveTrue("user1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userAddressService.getAddressDefault());
    }

    @Test
    void getAddressDefault_whenFound_thenReturnAddressDetail() {
        setAuthenticationName("user1");

        UserAddress active = UserAddress.builder().id(1L).userId("user1").addressId(100L).isActive(true).build();
        AddressDetailVm detailVm = AddressDetailVm.builder().id(100L).contactName("A").build();

        when(userAddressRepository.findByUserIdAndIsActiveTrue("user1")).thenReturn(Optional.of(active));
        when(locationService.getAddressById(100L)).thenReturn(detailVm);

        AddressDetailVm result = userAddressService.getAddressDefault();

        assertEquals(100L, result.id());
        assertEquals("A", result.contactName());
    }

    @Test
    void createAddress_whenFirstAddress_thenSavedAsActive() {
        setAuthenticationName("user1");

        AddressPostVm postVm = new AddressPostVm("A", "1", "line", "city", "zip", 1L, 2L, 3L);
        AddressVm createdAddress = AddressVm.builder().id(100L).contactName("A").build();

        when(userAddressRepository.findAllByUserId("user1")).thenReturn(List.of());
        when(locationService.createAddress(postVm)).thenReturn(createdAddress);
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress ua = invocation.getArgument(0);
            ua.setId(10L);
            return ua;
        });

        UserAddressVm result = userAddressService.createAddress(postVm);

        assertEquals("user1", result.userId());
        assertEquals(100L, result.addressGetVm().id());
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void createAddress_whenNotFirstAddress_thenSavedAsInactive() {
        setAuthenticationName("user1");

        AddressPostVm postVm = new AddressPostVm("A", "1", "line", "city", "zip", 1L, 2L, 3L);
        AddressVm createdAddress = AddressVm.builder().id(200L).contactName("B").build();

        UserAddress existing = UserAddress.builder().id(1L).userId("user1").addressId(100L).isActive(true).build();
        when(userAddressRepository.findAllByUserId("user1")).thenReturn(List.of(existing));
        when(locationService.createAddress(postVm)).thenReturn(createdAddress);
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress ua = invocation.getArgument(0);
            ua.setId(11L);
            return ua;
        });

        UserAddressVm result = userAddressService.createAddress(postVm);

        assertEquals("user1", result.userId());
        assertEquals(200L, result.addressGetVm().id());
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void deleteAddress_whenNotFound_thenThrowNotFoundException() {
        setAuthenticationName("user1");
        when(userAddressRepository.findOneByUserIdAndAddressId("user1", 99L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> userAddressService.deleteAddress(99L));
    }

    @Test
    void deleteAddress_whenFound_thenDelete() {
        setAuthenticationName("user1");
        UserAddress found = UserAddress.builder().id(1L).userId("user1").addressId(10L).isActive(true).build();
        when(userAddressRepository.findOneByUserIdAndAddressId("user1", 10L)).thenReturn(found);

        userAddressService.deleteAddress(10L);

        verify(userAddressRepository).delete(found);
    }

    @Test
    void chooseDefaultAddress_whenNormalCase_thenOnlyTargetBecomesActive() {
        setAuthenticationName("user1");

        UserAddress address100 = UserAddress.builder().id(1L).userId("user1").addressId(100L).isActive(true).build();
        UserAddress address200 = UserAddress.builder().id(2L).userId("user1").addressId(200L).isActive(false).build();
        when(userAddressRepository.findAllByUserId("user1")).thenReturn(List.of(address100, address200));

        userAddressService.chooseDefaultAddress(200L);

        assertThat(address100.getIsActive()).isFalse();
        assertThat(address200.getIsActive()).isTrue();
        verify(userAddressRepository).saveAll(List.of(address100, address200));
    }

    private void setAuthenticationName(String username) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }
}
