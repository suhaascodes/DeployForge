package com.deployforge.deployment.repository;

import com.deployforge.deployment.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {
    List<Deployment> findByProjectIdOrderByStartedAtDesc(UUID projectId);

    @Query("SELECT d FROM Deployment d JOIN d.project p WHERE p.owner.id = :ownerId ORDER BY d.startedAt DESC")
    List<Deployment> findByOwnerIdOrderByStartedAtDesc(@Param("ownerId") UUID ownerId);

    @Query("SELECT d FROM Deployment d JOIN FETCH d.project p JOIN FETCH p.owner o WHERE d.id = :id")
    java.util.Optional<Deployment> findByIdWithProjectAndOwner(@Param("id") UUID id);
}
