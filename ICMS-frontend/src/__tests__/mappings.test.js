import { describe, it, expect } from 'vitest';
import {
  getRoomTypeLabel, getRoomTypeTag,
  getClassroomStatusLabel, getClassroomStatusTag,
  getDeviceStatusLabel, getDeviceTypeLabel,
  getReservationStatusLabel, getReservationStatusTag,
  getWorkOrderStatusLabel, getFaultLevelLabel,
  getRoleLabel, getRoleTag, getMethodTag,
  getWeekdayLabel, getSectionTime
} from '../utils/mappings';

describe('mappings', () => {
  describe('room type', () => {
    it('getRoomTypeLabel', () => {
      expect(getRoomTypeLabel('normal')).toBe('普通教室');
      expect(getRoomTypeLabel('multimedia')).toBe('多媒体教室');
      expect(getRoomTypeLabel('lab')).toBe('实验室');
      expect(getRoomTypeLabel('unknown')).toBe('unknown');
    });
    it('getRoomTypeTag', () => {
      expect(getRoomTypeTag('normal')).toBe('info');
      expect(getRoomTypeTag('multimedia')).toBe('primary');
      expect(getRoomTypeTag('lab')).toBe('warning');
      expect(getRoomTypeTag('unknown')).toBe('info');
    });
  });
  describe('classroom status', () => {
    it('getClassroomStatusLabel', () => {
      expect(getClassroomStatusLabel('available')).toBe('可用');
      expect(getClassroomStatusLabel('maintenance')).toBe('维护中');
      expect(getClassroomStatusLabel('deactivated')).toBe('已停用');
    });
    it('getClassroomStatusTag', () => {
      expect(getClassroomStatusTag('available')).toBe('success');
      expect(getClassroomStatusTag('maintenance')).toBe('warning');
      expect(getClassroomStatusTag('deactivated')).toBe('danger');
    });
  });
  describe('device', () => {
    it('getDeviceStatusLabel', () => {
      expect(getDeviceStatusLabel('normal')).toBe('正常运转');
      expect(getDeviceStatusLabel('fault')).toBe('发生故障');
      expect(getDeviceStatusLabel('repairing')).toBe('正在维修');
      expect(getDeviceStatusLabel('scrapped')).toBe('已报废');
    });
    it('getDeviceTypeLabel', () => {
      expect(getDeviceTypeLabel('projector')).toBe('投影仪');
      expect(getDeviceTypeLabel('lock')).toBe('智能门禁');
      expect(getDeviceTypeLabel('air_conditioner')).toBe('空调');
    });
  });
  describe('reservation', () => {
    it('getReservationStatusLabel', () => {
      expect(getReservationStatusLabel('pending')).toBe('待审批');
      expect(getReservationStatusLabel('approved')).toBe('已通过');
      expect(getReservationStatusLabel('rejected')).toBe('已驳回');
      expect(getReservationStatusLabel('cancelled')).toBe('已取消');
    });
    it('getReservationStatusTag', () => {
      expect(getReservationStatusTag('pending')).toBe('warning');
      expect(getReservationStatusTag('approved')).toBe('success');
      expect(getReservationStatusTag('rejected')).toBe('danger');
    });
  });
  describe('work order', () => {
    it('getWorkOrderStatusLabel', () => {
      expect(getWorkOrderStatusLabel('submitted')).toBe('已提交');
      expect(getWorkOrderStatusLabel('processing')).toBe('处理中');
    });
    it('getFaultLevelLabel', () => {
      expect(getFaultLevelLabel('normal')).toBe('普通');
      expect(getFaultLevelLabel('urgent')).toBe('紧急');
      expect(getFaultLevelLabel('severe')).toBe('严重');
    });
  });
  describe('role', () => {
    it('getRoleLabel', () => {
      expect(getRoleLabel('admin')).toBe('系统管理员');
      expect(getRoleLabel('teacher')).toBe('教师');
      expect(getRoleLabel('student')).toBe('学生');
    });
    it('getRoleTag', () => {
      expect(getRoleTag('admin')).toBe('danger');
      expect(getRoleTag('repairer')).toBe('info');
    });
  });
  describe('utility mappers', () => {
    it('getMethodTag', () => {
      expect(getMethodTag('GET')).toBe('info');
      expect(getMethodTag('POST')).toBe('success');
      expect(getMethodTag('DELETE')).toBe('danger');
    });
    it('getWeekdayLabel', () => {
      expect(getWeekdayLabel(1)).toBe('周一');
      expect(getWeekdayLabel(7)).toBe('周日');
      expect(getWeekdayLabel(0)).toBe('');
    });
    it('getSectionTime', () => {
      expect(getSectionTime(1, 2)).toBe('08:00-09:35');
      expect(getSectionTime(4, 5)).toBe('11:40-14:00');
    });
  });
});