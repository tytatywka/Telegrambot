package com.example.aorrbot.repository;

import com.example.aorrbot.model.Ads;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdsRepository extends JpaRepository<Ads,Long> {

}

