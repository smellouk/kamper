import 'package:flutter_test/flutter_test.dart';
import 'package:konitor_flutter_demo/main.dart';

void main() {
  testWidgets('KonitorDemoApp smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const KonitorDemoApp());
    expect(find.text('CPU'), findsOneWidget);
  });
}
