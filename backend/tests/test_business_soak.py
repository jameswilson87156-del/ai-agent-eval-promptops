import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


TOOLS_DIR = Path(__file__).resolve().parents[1] / "tools"
sys.path.insert(0, str(TOOLS_DIR))
import run_business_soak


class BusinessSoakSafetyTests(unittest.TestCase):
    def test_local_loopback_target_is_accepted(self):
        self.assertEqual(
            run_business_soak.require_loopback("http://127.0.0.1:18083"),
            "http://127.0.0.1:18083",
        )

    def test_compose_service_requires_explicit_flag_and_exact_host_port(self):
        with self.assertRaises(ValueError):
            run_business_soak.require_loopback("http://backend:18083")
        self.assertEqual(
            run_business_soak.require_loopback("http://backend:18083", allow_compose_service=True),
            "http://backend:18083",
        )
        with self.assertRaises(ValueError):
            run_business_soak.require_loopback("http://backend:18084", allow_compose_service=True)

    def test_non_loopback_dns_result_is_rejected(self):
        fake_result = [(None, None, None, None, ("203.0.113.20", 80))]
        with patch.object(run_business_soak.socket, "getaddrinfo", return_value=fake_result):
            with self.assertRaisesRegex(ValueError, "loopback"):
                run_business_soak.require_loopback("http://api.example.test")

    def test_existing_evidence_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            report_path = Path(directory) / "report.json"
            run_business_soak.write_report(report_path, {"first": True}, initial=True)
            original = report_path.read_text(encoding="utf-8")
            with self.assertRaises(FileExistsError):
                run_business_soak.write_report(report_path, {"first": False}, initial=True)
            self.assertEqual(report_path.read_text(encoding="utf-8"), original)


if __name__ == "__main__":
    unittest.main()
