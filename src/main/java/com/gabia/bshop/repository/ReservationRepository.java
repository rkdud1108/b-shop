package com.gabia.bshop.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gabia.bshop.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	Optional<Reservation> findByItem_Id(final Long itemId);

	@Query("""
		select r
		from Reservation r
		join fetch r.item
		where r.item.openAt <= :now
		"""
	)
	List<Reservation> findAllByItemOpenAtBefore(final LocalDateTime now);
}