create table public.barcodes (
  barcode text not null,
  food_id uuid null,
  constraint barcodes_pkey primary key (barcode),
  constraint barcodes_food_id_fkey foreign KEY (food_id) references foods (id) on delete CASCADE
) TABLESPACE pg_default;

create index IF not exists idx_barcodes_barcode on public.barcodes using btree (barcode) TABLESPACE pg_default;

create index IF not exists idx_barcodes_food_id on public.barcodes using btree (food_id) TABLESPACE pg_default;

create table public.exercise_sets (
  id uuid not null default extensions.uuid_generate_v4 (),
  workout_exercise_id uuid null,
  weight numeric(5, 2) null default '0'::numeric,
  reps integer null default 0,
  is_completed boolean null default false,
  created_at timestamp with time zone null default now(),
  set_number integer null default 1,
  constraint exercise_sets_pkey primary key (id),
  constraint exercise_sets_workout_exercise_id_fkey foreign KEY (workout_exercise_id) references workout_exercises (id) on delete CASCADE,
  constraint fk_workout_exercise foreign KEY (workout_exercise_id) references workout_exercises (id) on delete CASCADE
) TABLESPACE pg_default;

create index IF not exists idx_exercise_sets_workout_exercise_id on public.exercise_sets using btree (workout_exercise_id) TABLESPACE pg_default;

create table public.exercises (
  id uuid not null default extensions.uuid_generate_v4 (),
  name character varying not null,
  description text null,
  exercise_type character varying null,
  difficulty character varying null,
  equipment_required character varying[] null,
  primary_muscles character varying[] null,
  secondary_muscles character varying[] null,
  stabilizer_muscles character varying[] null,
  met double precision null,
  instructions text null,
  common_mistakes text[] null,
  contraindications text[] null,
  media jsonb null default '{"animation_url": null, "preview_image": null}'::jsonb,
  category character varying null,
  constraint exercises_pkey primary key (id),
  constraint exercises_name_key unique (name)
) TABLESPACE pg_default;

create table public.foods (
  name text not null,
  category text not null,
  subcategory text not null,
  calories integer not null,
  proteins real not null,
  fats real not null,
  carbs real not null,
  popularity integer null,
  calcium real null,
  iron real null,
  magnesium real null,
  phosphorus real null,
  potassium real null,
  sodium real null,
  zinc real null,
  vitamin_a real null,
  vitamin_b1 real null,
  vitamin_b2 real null,
  vitamin_b3 real null,
  vitamin_b5 real null,
  vitamin_b6 real null,
  vitamin_b9 real null,
  vitamin_b12 real null,
  vitamin_c real null,
  vitamin_d real null,
  vitamin_e real null,
  vitamin_k real null,
  cholesterol real null,
  saturated_fats real null,
  trans_fats real null,
  fiber real null,
  sugar real null,
  usefulness_index integer not null default 5,
  is_moderated boolean null default false,
  id uuid not null,
  constraint foods_pkey primary key (id),
  constraint foods_id_key unique (id),
  constraint foods_name_key unique (name),
  constraint foods_usefulness_index_check check (
    (
      (usefulness_index >= 1)
      and (usefulness_index <= 10)
    )
  )
) TABLESPACE pg_default;

create index IF not exists idx_foods_name on public.foods using btree (name) TABLESPACE pg_default;

create index IF not exists idx_foods_category on public.foods using btree (category) TABLESPACE pg_default;

create index IF not exists idx_foods_popularity on public.foods using btree (popularity desc) TABLESPACE pg_default;

create index IF not exists idx_foods_subcategory on public.foods using btree (subcategory) TABLESPACE pg_default;

create index IF not exists idx_foods_category_subcategory on public.foods using btree (category, subcategory) TABLESPACE pg_default;

create index IF not exists idx_foods_usefulness on public.foods using btree (usefulness_index) TABLESPACE pg_default;

create index IF not exists idx_foods_calories on public.foods using btree (calories) TABLESPACE pg_default;

create index IF not exists idx_foods_category_usefulness on public.foods using btree (category, usefulness_index) TABLESPACE pg_default;

create index IF not exists idx_foods_macros on public.foods using btree (proteins, fats, carbs) TABLESPACE pg_default;

create table public.program_days (
  id uuid not null default extensions.uuid_generate_v4 (),
  program_id uuid null,
  day_number integer not null,
  name text not null,
  description text null,
  template_day_id uuid null,
  created_at timestamp with time zone null default now(),
  updated_at timestamp with time zone null default now(),
  constraint program_days_pkey primary key (id),
  constraint program_days_program_id_fkey foreign KEY (program_id) references programs (id) on delete CASCADE,
  constraint program_days_template_day_id_fkey foreign KEY (template_day_id) references program_template_days (id)
) TABLESPACE pg_default;

create table public.program_exercises (
  id uuid not null default extensions.uuid_generate_v4 (),
  day_id uuid null,
  exercise_id uuid null,
  order_number integer not null,
  target_sets integer not null,
  target_reps text not null,
  target_weight text null,
  rest_seconds integer null,
  template_exercise_id uuid null,
  notes text null,
  created_at timestamp with time zone null default now(),
  updated_at timestamp with time zone null default now(),
  constraint program_exercises_pkey primary key (id),
  constraint program_exercises_day_id_fkey foreign KEY (day_id) references program_days (id) on delete CASCADE,
  constraint program_exercises_exercise_id_fkey foreign KEY (exercise_id) references exercises (id),
  constraint program_exercises_template_exercise_id_fkey foreign KEY (template_exercise_id) references program_template_exercises (id)
) TABLESPACE pg_default;

create table public.program_template_days (
  id uuid not null default extensions.uuid_generate_v4 (),
  template_id uuid null,
  day_number integer not null,
  name text not null,
  description text null,
  created_at timestamp with time zone null default now(),
  updated_at timestamp with time zone null default now(),
  constraint program_template_days_pkey primary key (id),
  constraint program_template_days_template_id_fkey foreign KEY (template_id) references program_templates (id) on delete CASCADE
) TABLESPACE pg_default;

create table public.program_template_exercises (
  id uuid not null default extensions.uuid_generate_v4 (),
  day_id uuid null,
  exercise_id uuid null,
  order_number integer not null,
  target_sets integer not null,
  target_reps text not null,
  target_weight text null,
  rest_seconds integer null,
  notes text null,
  created_at timestamp with time zone null default now(),
  updated_at timestamp with time zone null default now(),
  constraint program_template_exercises_pkey primary key (id),
  constraint program_template_exercises_day_id_fkey foreign KEY (day_id) references program_template_days (id) on delete CASCADE,
  constraint program_template_exercises_exercise_id_fkey foreign KEY (exercise_id) references exercises (id)
) TABLESPACE pg_default;

create table public.program_templates (
  id uuid not null default extensions.uuid_generate_v4 (),
  name text not null,
  description text null,
  type text not null,
  duration_weeks integer not null,
  days_per_week integer not null,
  difficulty text null,
  category text null,
  is_public boolean null default false,
  author_id uuid null,
  created_at timestamp with time zone null default now(),
  updated_at timestamp with time zone null default now(),
  constraint program_templates_pkey primary key (id)
) TABLESPACE pg_default;

create table public.programs (
  id uuid not null default extensions.uuid_generate_v4 (),
  user_id uuid not null,
  name text not null,
  description text null,
  type text not null,
  duration_weeks integer not null,
  days_per_week integer not null,
  is_active boolean null default false,
  start_date timestamp with time zone null,
  created_at timestamp with time zone null default now(),
  updated_at timestamp with time zone null default now(),
  template_id uuid null,
  periodization_type text null,
  current_phase text null,
  current_phase_start_date timestamp with time zone null,
  progression_type text null default 'LINEAR'::text,
  constraint programs_pkey primary key (id),
  constraint programs_template_id_fkey foreign KEY (template_id) references program_templates (id),
  constraint programs_user_id_fkey foreign KEY (user_id) references auth.users (id),
  constraint programs_days_per_week_check check (
    (
      (days_per_week >= 1)
      and (days_per_week <= 7)
    )
  ),
  constraint programs_duration_weeks_check check ((duration_weeks > 0)),
  constraint programs_type_check check (
    (
      type = any (
        array[
          'OFFICIAL'::text,
          'CUSTOM'::text,
          'AI_GENERATED'::text
        ]
      )
    )
  )
) TABLESPACE pg_default;

create index IF not exists idx_programs_user_id on public.programs using btree (user_id) TABLESPACE pg_default;

create trigger update_programs_updated_at BEFORE
update on programs for EACH row
execute FUNCTION update_updated_at_column ();

create table public.users (
  id uuid not null,
  name text null,
  target_calories integer null default 2000,
  created_at timestamp with time zone not null default timezone ('utc'::text, now()),
  age integer null,
  gender text null,
  fitness_goal text null,
  height numeric(5, 2) null,
  current_weight numeric(5, 2) null,
  target_weight numeric(5, 2) null,
  fitness_level text null,
  bmi numeric(5, 2) null,
  is_metric boolean null default true,
  updated_at timestamp with time zone null default now(),
  constraint users_pkey primary key (id),
  constraint users_id_fkey foreign KEY (id) references auth.users (id) on delete CASCADE
) TABLESPACE pg_default;

create table public.workout_exercises (
  id uuid not null default extensions.uuid_generate_v4 (),
  workout_id uuid null,
  exercise_id uuid null,
  order_number integer null,
  rest_seconds integer null default 60,
  notes text null,
  created_at timestamp with time zone null default now(),
  constraint workout_exercises_pkey primary key (id),
  constraint fk_workout foreign KEY (workout_id) references workouts (id) on delete CASCADE,
  constraint workout_exercises_exercise_id_fkey foreign KEY (exercise_id) references exercises (id),
  constraint workout_exercises_workout_id_fkey foreign KEY (workout_id) references workouts (id) on delete CASCADE
) TABLESPACE pg_default;

create index IF not exists idx_workout_exercises_workout_id on public.workout_exercises using btree (workout_id) TABLESPACE pg_default;

create table public.workout_plans (
  id uuid not null default extensions.uuid_generate_v4 (),
  user_id uuid null,
  name text null,
  planned_date timestamp with time zone not null,
  program_id uuid null,
  program_day_id uuid null,
  status text null default 'planned'::text,
  notes text null,
  created_at timestamp with time zone null default now(),
  updated_at timestamp with time zone null default now(),
  constraint workout_plans_pkey primary key (id),
  constraint workout_plans_program_day_id_fkey foreign KEY (program_day_id) references program_days (id),
  constraint workout_plans_program_id_fkey foreign KEY (program_id) references programs (id),
  constraint workout_plans_user_id_fkey foreign KEY (user_id) references auth.users (id)
) TABLESPACE pg_default;

create table public.workouts (
  id uuid not null default extensions.uuid_generate_v4 (),
  user_id uuid null,
  name text null,
  start_time timestamp with time zone null,
  total_calories integer null default 0,
  notes text null,
  created_at timestamp with time zone null default now(),
  updated_at timestamp with time zone null default now(),
  end_time timestamp with time zone null,
  program_id uuid null,
  program_day_number integer null,
  plan_id uuid null,
  constraint workouts_pkey primary key (id),
  constraint workouts_plan_id_fkey foreign KEY (plan_id) references workout_plans (id),
  constraint workouts_program_id_fkey foreign KEY (program_id) references programs (id),
  constraint workouts_user_id_fkey foreign KEY (user_id) references auth.users (id),
  constraint program_fields_consistency check (
    (
      (
        (program_id is null)
        and (program_day_number is null)
      )
      or (
        (program_id is not null)
        and (program_day_number is not null)
      )
    )
  )
) TABLESPACE pg_default;

create index IF not exists idx_workouts_user_id on public.workouts using btree (user_id) TABLESPACE pg_default;

create index IF not exists idx_workouts_start_time on public.workouts using btree (start_time) TABLESPACE pg_default;

create index IF not exists idx_workouts_program on public.workouts using btree (program_id, program_day_number) TABLESPACE pg_default
where
  (program_id is not null);