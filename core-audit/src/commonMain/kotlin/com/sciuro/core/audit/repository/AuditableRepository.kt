package com.sciuro.core.audit.repository

abstract class AuditableRepository(
    protected val auditRepository: AuditRepository
)
