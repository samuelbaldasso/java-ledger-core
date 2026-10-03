package com.sbaldasso.ledgercore.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128, updatable = false)
    private String idempotencyKey;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntryJpaEntity> entries = new ArrayList<>();

    protected TransactionJpaEntity() {
    }

    public TransactionJpaEntity(UUID id, String idempotencyKey, String description, Instant createdAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
        this.createdAt = createdAt;
    }

    public void addEntry(EntryJpaEntity entry) {
        entries.add(entry);
        entry.setTransaction(this);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<EntryJpaEntity> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryJpaEntity> entries) {
        this.entries = entries;
    }
}
