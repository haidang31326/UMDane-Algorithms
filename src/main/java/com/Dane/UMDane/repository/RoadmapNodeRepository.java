package com.Dane.UMDane.repository;

import com.Dane.UMDane.entity.RoadmapNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadmapNodeRepository extends JpaRepository<RoadmapNode, Integer> {
    List<RoadmapNode> findAllByOrderByNodeIdAsc();
}
