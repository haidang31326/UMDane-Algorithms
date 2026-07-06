package com.Dane.UMDane.repository;

import com.Dane.UMDane.entity.UserRestreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRestreakRepository extends JpaRepository<UserRestreak, Long> {
}
