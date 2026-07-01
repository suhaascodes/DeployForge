package com.deployforge.logging.repository;

import com.deployforge.logging.entity.DeploymentLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends MongoRepository<DeploymentLog, String> {
    List<DeploymentLog> findByDeploymentIdOrderByTimestampAsc(String deploymentId);
}
