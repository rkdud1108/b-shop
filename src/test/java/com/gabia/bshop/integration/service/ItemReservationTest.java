package com.gabia.bshop.integration.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.gabia.bshop.dto.request.ReservationUpdateRequest;
import com.gabia.bshop.dto.response.ItemReservationResponse;
import com.gabia.bshop.entity.Category;
import com.gabia.bshop.entity.Item;
import com.gabia.bshop.entity.Reservation;
import com.gabia.bshop.entity.enumtype.ItemStatus;
import com.gabia.bshop.exception.NotFoundException;
import com.gabia.bshop.fixture.CategoryFixture;
import com.gabia.bshop.fixture.ItemFixture;
import com.gabia.bshop.repository.CategoryRepository;
import com.gabia.bshop.repository.ItemRepository;
import com.gabia.bshop.repository.ReservationRepository;
import com.gabia.bshop.schedule.ReservationUpdateScheduler;
import com.gabia.bshop.service.ItemReserveService;

import jakarta.persistence.EntityManager;

@Transactional
@SpringBootTest
class ItemReservationTest {

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private ItemReserveService itemReserveService;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ReservationUpdateScheduler reservationUpdateScheduler;

	@Autowired
	private EntityManager entityManager;

	@DisplayName("아이템의 오픈을 예약한다.")
	@Test
	void itemOpenReservation() {
		// given
		final LocalDateTime now = LocalDateTime.now();

		final Category category = CategoryFixture.CATEGORY_1.getInstance();
		categoryRepository.save(category);

		final Item item = ItemFixture.ITEM_1.getInstance(category);
		itemRepository.save(item);
		final Long expected = item.getId();

		final ReservationUpdateRequest reservationUpdateRequest = new ReservationUpdateRequest(now.plusMinutes(1L));

		// when
		ItemReservationResponse itemReservationResponse = itemReserveService.createItemReservation(item.getId(),
			reservationUpdateRequest);
		final Long actual = itemReservationResponse.itemId();

		// then
		Assertions.assertEquals(expected, actual);
	}

	@DisplayName("아이템의 오픈예약을 수정한다.")
	@Test
	void updateOpenReservation() {
		// given
		final LocalDateTime now = LocalDateTime.now();

		final Category category = CategoryFixture.CATEGORY_1.getInstance();
		categoryRepository.save(category);

		final Item item = ItemFixture.ITEM_1.getInstance(category);
		itemRepository.save(item);

		final ReservationUpdateRequest reservationUpdateRequest = new ReservationUpdateRequest(now.plusMinutes(1L));
		final LocalDateTime expected = reservationUpdateRequest.openAt();
		final Reservation reservation = Reservation.builder().item(item).build();
		reservationRepository.save(reservation);
		// when
		itemReserveService.updateItemReservation(item.getId(), reservationUpdateRequest);
		final LocalDateTime actual = itemRepository.findById(item.getId()).orElseThrow().getOpenAt();

		// then
		Assertions.assertEquals(expected, actual);
	}

	@DisplayName("아이템의 오픈예약을 취소한다.")
	@Test
	void removeOpenReservation() {
		// given
		final Category category = CategoryFixture.CATEGORY_1.getInstance();
		categoryRepository.save(category);

		final Item item = ItemFixture.ITEM_1.getInstance(category);
		itemRepository.save(item);

		final Reservation reservation = Reservation.builder().item(item).build();
		reservationRepository.save(reservation);
		// when

		itemReserveService.removeReservation(item.getId());
		// then
		Assertions.assertThrows(
			NotFoundException.class,
			() -> {
				itemReserveService.findItemReservation(item.getId());
			});

	}

	@DisplayName("아이템의 예약 상태를 조회한다.")
	@Test
	void findOpenReservation() {
		// given
		final LocalDateTime now = LocalDateTime.now();

		final Category category = CategoryFixture.CATEGORY_1.getInstance();
		categoryRepository.save(category);

		final Item item = ItemFixture.ITEM_1.getInstance(category);
		item.setOpenAt(now.plusMinutes(1L));
		final Long expected = itemRepository.save(item).getId();

		final Reservation reservation = Reservation.builder().item(item).build();
		reservationRepository.save(reservation);
		// when
		final Long actual = itemReserveService.findItemReservation(item.getId()).itemId();

		// then
		Assertions.assertEquals(expected, actual);
	}

	@Disabled("현재 동시성 테스트와 같이 실행하면 카테고리 조회시에 안되는 통합테스트의 문제가 발생한다.")
	@DisplayName("아이템 예약 스케줄이 실행되면 아이탬 상태가 변화하는지 확인한다.")
	@Test
	void reservationScheduleValidation() {
		// given
		final LocalDateTime now = LocalDateTime.now();

		final Category category = CategoryFixture.CATEGORY_1.getInstance();
		categoryRepository.save(category);

		final Item item1 = ItemFixture.ITEM_1.getInstance(category);
		final Item item2 = ItemFixture.ITEM_2.getInstance(category);

		item1.setOpenAt(now.plusMinutes(10L));
		item1.setItemStatus(ItemStatus.RESERVED);

		item2.setOpenAt(now.minusMinutes(1L));
		item2.setItemStatus(ItemStatus.RESERVED);

		itemRepository.saveAll(List.of(item1, item2));

		final Reservation reservation1 = Reservation.builder().item(item1).build();
		final Reservation reservation2 = Reservation.builder().item(item2).build();

		reservationRepository.saveAll(List.of(reservation1, reservation2));

		// when
		reservationUpdateScheduler.updateReservationStatus();

		entityManager.flush();
		entityManager.clear();

		// then
		Assertions.assertAll(
			() -> {
				Assertions.assertEquals(ItemStatus.RESERVED,
					itemRepository.findById(item1.getId()).orElseThrow().getItemStatus());
			},
			() -> {
				Assertions.assertEquals(ItemStatus.PUBLIC,
					itemRepository.findById(item2.getId()).orElseThrow().getItemStatus());
			},
			() -> {
				Assertions.assertThrows(
					NotFoundException.class,
					() -> {
						itemReserveService.findItemReservation(item2.getId());
					});
			}
		);
	}
}
