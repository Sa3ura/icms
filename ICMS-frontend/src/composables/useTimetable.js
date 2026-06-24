import { computed } from 'vue';

export function useTimetable(schedules, reservations) {
  const timetableGrid = computed(() => {
    const grid = [];
    const timeLabels = {
      1: '1-2节\n08:00-09:35',
      3: '3-4节\n10:05-11:40',
      5: '5-6节\n14:00-15:35',
      7: '7-8节\n16:05-17:40',
      9: '9-10节\n19:00-20:35',
      11: '11-12节\n21:05-22:00'
    };

    for (let s = 1; s <= 11; s += 2) {
      const row = { section: s, section_label: timeLabels[s] };

      for (let d = 1; d <= 7; d++) {
        const course = schedules.value.find(sc =>
          Number(sc.weekday) === d &&
          Number(sc.start_section) <= s + 1 &&
          Number(sc.end_section) >= s
        );

        if (course) {
          row[d] = {
            type: 'course',
            name: course.course_name,
            teacher: course.teacher_name,
            details: `班级: ${course.class_name || ''}`
          };
          continue;
        }

        const res = reservations.value.find(r => {
          const day = new Date(r.reservation_date).getDay() || 7;
          return day === d &&
            Number(r.start_section) <= s + 1 &&
            Number(r.end_section) >= s;
        });

        if (res) {
          row[d] = {
            type: 'reservation',
            name: `[约] ${res.purpose}`,
            teacher: res.applicant_name,
            details: `日期: ${res.reservation_date}`
          };
        }
      }
      grid.push(row);
    }
    return grid;
  });

  return { timetableGrid };
}
