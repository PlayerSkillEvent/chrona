package dev.chrona.job.core;

import dev.chrona.economy.EconomyService;

import javax.sql.DataSource;

public record JobContext(DataSource ds, EconomyService econ, JobConfigProvider config) {}
