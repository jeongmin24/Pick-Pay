package com.ssafy.pickpay.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.pickpay.domain.Menu;

import jakarta.persistence.LockModeType;

public interface MenuRepository extends JpaRepository<Menu, Long> {
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select m
			from Menu m
			where m.menuId = :menuId
			""")
	Optional<Menu> findByMenuIdForUpdate(@Param("menuId") Long menuId);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select m
        from Menu m
        where m.menuId in :menuIds
        order by m.menuId asc
    """)
    List<Menu> findAllByMenuIdsForUpdate(@Param("menuIds") List<Long> menuIds);
	
	Optional<Menu> findByName(String name);

}
