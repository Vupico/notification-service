// Usage:
//   mongosh "mongodb://localhost:27017/notification" notification-service/scripts/mongo-init.js
//
// Creates collections + indexes and upserts sample tenant/template docs.

const dbName = db.getName();
print(`Using database: ${dbName}`);

// 1) Collections (explicit)
db.createCollection("tenant_configuration");
db.createCollection("email_template");

// 2) Indexes
db.email_template.createIndex(
  { tenantId: 1, templateName: 1 },
  { name: "ux_email_template_tenant_name", unique: true }
);

db.tenant_configuration.createIndex(
  { tenantId: 1 },
  { name: "ux_tenant_configuration_tenantId", unique: true }
);

// 3) Seed data
const now = new Date();

db.tenant_configuration.updateOne(
  { tenantId: "tenant_101" },
  {
    $set: {
      tenantId: "tenant_101",
      emailHost: "ses",
      emailRateLimit: 10,
      retryCount: 3,
      retryInterval: 10,
    },
  },
  { upsert: true }
);

db.email_template.updateOne(
  { tenantId: "tenant_101", templateName: "defect_logged" },
  {
    $set: {
      subject: "Defect Logged: {{ticket_id}} — {{defect_title}} ({{severity}})",
      body:
        "Dear Team,\n\n" +
        "A new defect has been logged in {{application_name}}.\n\n" +
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

db.email_template.updateOne(
  { tenantId: "tenant_101", templateName: "change_request_logged" },
  {
    $set: {
      subject: "Change Request Logged: {{ticket_id}} — {{defect_title}} ({{severity}})",
      body:
        "Dear Team,\n\n" +
        "A new change request has been logged in {{application_name}}.\n\n" +
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

print("Done. Seeded tenant_configuration + email_template.");

