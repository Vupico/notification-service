/**
 * MongoDB seed script — insert supplier survey email templates.
 *
 * Usage (mongosh):
 *   mongosh "<connection-string>" --file survey_email_templates.js
 *
 * Or from the shell:
 *   mongosh mongodb://localhost:27017/dap --file survey_email_templates.js
 *
 * Templates use {{placeholder}} syntax resolved by TemplateServiceImpl.
 *
 * Available placeholders per message_type:
 *
 *  survey_assigned:  supplier_name, survey_name, survey_url, start_date, end_date, sent_by
 *  survey_completed: survey_name, supplier_code, survey_url
 *  survey_reminder:  supplier_name, survey_name, survey_url, end_date
 */

const db = connect(db.getName ? db.getName() : "dap");

const col = db.getCollection("notification_template");

const templates = [
  {
    notification_type: "email",
    message_type: "survey_assigned",
    version: "v1",
    subject: "Action Required: Survey Assigned – {{survey_name}}",
    body: `Dear {{supplier_name}},

A new survey has been assigned to you by {{sent_by}}.

Survey:     {{survey_name}}
Start Date: {{start_date}}
Deadline:   {{end_date}}

Please complete it at your earliest convenience:
{{survey_url}}

If you have any questions, please contact your administrator.

Regards,
Vupico Team`
  },
  {
    notification_type: "email",
    message_type: "survey_completed",
    version: "v1",
    subject: "Survey Completed – {{survey_name}}",
    body: `Hello,

Supplier {{supplier_code}} has completed the survey "{{survey_name}}".

You can review the submitted responses here:
{{survey_url}}

Regards,
Vupico Team`
  },
  {
    notification_type: "email",
    message_type: "survey_reminder",
    version: "v1",
    subject: "Reminder: Survey Deadline Approaching – {{survey_name}}",
    body: `Dear {{supplier_name}},

This is a friendly reminder that the deadline for your survey is approaching.

Survey:   {{survey_name}}
Deadline: {{end_date}}

Please complete it before the deadline:
{{survey_url}}

If you have already submitted, please disregard this message.

Regards,
Vupico Team`
  }
];

templates.forEach(function(tmpl) {
  const result = col.updateOne(
    {
      notification_type: tmpl.notification_type,
      message_type: tmpl.message_type,
      version: tmpl.version
    },
    { $setOnInsert: tmpl },
    { upsert: true }
  );

  if (result.upsertedCount > 0) {
    print("Inserted template: " + tmpl.message_type);
  } else {
    print("Template already exists (skipped): " + tmpl.message_type);
  }
});

print("Done.");
