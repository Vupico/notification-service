// Usage:
//   mongosh "mongodb://localhost:27017/notification" notification-service/scripts/mongo-init.js
//
// Creates collections + indexes and upserts sample tenant/template docs.

const dbName = db.getName();
print(`Using database: ${dbName}`);

// 1) Collections (explicit)
db.createCollection("tenant_configuration");
db.createCollection("notification_template");

// 2) Indexes
db.notification_template.createIndex(
  { tenantId: 1, notificationType: 1, messageType: 1, version: 1 },
  { name: "ux_notification_template_lookup", unique: true }
);

db.tenant_configuration.createIndex(
  { tenantId: 1 },
  { name: "ux_tenant_configuration_tenantId", unique: true }
);

// 3) Seed data
const now = new Date();

db.tenant_configuration.updateOne(
  { tenantId: "CA_3ygaHfx252" },
  {
    $set: {
      tenantId: "tenant_101",
      emailHost: "ses",
      emailRateLimit: 10,
      retryCount: 3,
      retryInterval: 10,
      "properties.allowedSeverities": [1, 2, 3, 4, 5],
      "properties.severityToSlaHours": { "1": 24, "2": 48, "3": 72, "4": 96, "5": 120 },
    },
  },
  { upsert: true }
);

db.notification_template.updateOne(
  {
    tenantId: "CA_3ygaHfx252",
    notificationType: "email",
    messageType: "defect_logged",
    version: "v1",
  },
  {
    $set: {
      subject: "Defect Logged: {{ticket_id}} — {{defect_title}} ({{severity}})",
      body:
        "Dear Team,\n\n" +
        "A new defect has been logged in {{application_name}}.\n\n" +
        "Ticket URL: {{ticket_url}}\n\n" +
        "Defect Details\n" +
        "- Ticket ID: {{ticket_id}}\n" +
        "- Title: {{defect_title}}\n" +
        "- Severity: {{severity}}\n" +
        "- Reported by: {{reported_by}}\n" +
        "- Reported at: {{reported_at}}\n\n" +
        "{{description}}\n\n" +
        "Please review and take the necessary action at the earliest.\n\n" +
        "Regards,\n" +
        "{{reported_by}}",
      updatedAt: now,
    },
    $setOnInsert: {
      createdAt: now,
    },
  },
  { upsert: true }
);

db.notification_template.updateOne(
  {
    tenantId: "CA_3ygaHfx252",
    notificationType: "email",
    messageType: "change_request_logged",
    version: "v1",
  },
  {
    $set: {
      subject: "Change Request Logged: {{ticket_id}} — {{defect_title}} ({{severity}})",
      body:
        "Dear Team,\n\n" +
        "A new change request has been logged in {{application_name}}.\n\n" +
        "Ticket URL: {{ticket_url}}\n\n" +
        "Request details\n" +
        "- Ticket ID: {{ticket_id}}\n" +
        "- Subject: {{defect_title}}\n" +
        "- Priority: {{severity}}\n" +
        "- Raised by: {{reported_by}}\n" +
        "- Raised at: {{reported_at}}\n\n" +
        "{{description}}\n\n" +
        "Please review and process this change request.\n\n" +
        "Regards,\n" +
        "{{reported_by}}",
      updatedAt: now,
    },
    $setOnInsert: {
      createdAt: now,
    },
  },
  { upsert: true }
);

db.notification_template.updateOne(
  {
    tenantId: "CA_3ygaHfx252",
    notificationType: "email",
    messageType: "defect_updated",
    version: "v1",
  },
  {
    $set: {
      subject: "Defect Updated: {{ticket_id}}",
      body:
        "A defect ticket has been updated.\n\n" +
        "Ticket URL: {{ticket_url}}\n" +
        "Updated by: {{update_by}}\n" +
        "Timestamp: {{timestamp}}\n\n" +
        "Update:\n" +
        "{{update}}\n",
      updatedAt: now,
    },
    $setOnInsert: {
      createdAt: now,
    },
  },
  { upsert: true }
);

print("Done. Seeded tenant_configuration + notification_template.");
