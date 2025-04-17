package com.mpjmp.fileupload.repository;

import com.dataorchestrate.common.Device;
import com.dataorchestrate.common.DeviceRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Repository("commonDeviceRepository")
public class CommonDeviceRepositoryImpl implements DeviceRepository {

    private final MongoTemplate mongoTemplate;

    public CommonDeviceRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Device> findByMacAddress(String macAddress) {
        Query query = new Query(Criteria.where("macAddress").is(macAddress));
        Device device = mongoTemplate.findOne(query, Device.class);
        return Optional.ofNullable(device);
    }

    @Override
    public Optional<Device> findByDeviceName(String deviceName) {
        Query query = new Query(Criteria.where("deviceName").is(deviceName));
        Device device = mongoTemplate.findOne(query, Device.class);
        return Optional.ofNullable(device);
    }

    @Override
    public <S extends Device> S save(S entity) {
        mongoTemplate.save(entity);
        return entity;
    }

    @Override
    public <S extends Device> List<S> saveAll(Iterable<S> entities) {
        entities.forEach(mongoTemplate::save);
        return (List<S>) entities;
    }

    @Override
    public Optional<Device> findById(String s) {
        return Optional.ofNullable(mongoTemplate.findById(s, Device.class));
    }

    @Override
    public boolean existsById(String s) {
        return mongoTemplate.exists(new Query(Criteria.where("id").is(s)), Device.class);
    }

    @Override
    public List<Device> findAll() {
        return mongoTemplate.findAll(Device.class);
    }

    @Override
    public List<Device> findAll(Sort sort) {
        return mongoTemplate.find(new Query().with(sort), Device.class);
    }

    @Override
    public Page<Device> findAll(Pageable pageable) {
        long total = mongoTemplate.count(new Query(), Device.class);
        List<Device> content = mongoTemplate.find(new Query().with(pageable), Device.class);
        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Device> findAllById(Iterable<String> strings) {
        Query query = new Query(Criteria.where("_id").in(strings));
        return mongoTemplate.find(query, Device.class);
    }

    @Override
    public long count() {
        return mongoTemplate.count(new Query(), Device.class);
    }

    @Override
    public void deleteById(String s) {
        Query query = new Query(Criteria.where("_id").is(s));
        mongoTemplate.remove(query, Device.class);
    }

    @Override
    public void delete(Device entity) {
        mongoTemplate.remove(entity);
    }

    @Override
    public void deleteAllById(Iterable<? extends String> strings) {
        Query query = new Query(Criteria.where("_id").in(strings));
        mongoTemplate.remove(query, Device.class);
    }

    @Override
    public void deleteAll(Iterable<? extends Device> entities) {
        entities.forEach(mongoTemplate::remove);
    }

    @Override
    public void deleteAll() {
        mongoTemplate.remove(new Query(), Device.class);
    }

    // These methods are part of the MongoRepository interface but we won't use them
    // Providing minimal implementations to satisfy the interface

    @Override
    public <S extends Device> S insert(S entity) {
        mongoTemplate.insert(entity);
        return entity;
    }

    @Override
    public <S extends Device> List<S> insert(Iterable<S> entities) {
        entities.forEach(mongoTemplate::insert);
        return (List<S>) entities;
    }

    @Override
    public <S extends Device> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends Device> List<S> findAll(Example<S> example) {
        return List.of();
    }

    @Override
    public <S extends Device> List<S> findAll(Example<S> example, Sort sort) {
        return List.of();
    }

    @Override
    public <S extends Device> Page<S> findAll(Example<S> example, Pageable pageable) {
        return Page.empty();
    }

    @Override
    public <S extends Device> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends Device> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends Device, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }
}
